package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.FollowStats;
import com.project.bookreviewer.application.dto.response.UserSearchItemDto;
import com.project.bookreviewer.domain.event.FollowCreatedEvent;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.config.FeedBackfillProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepositoryPort followRepository;
    private final UserService userService;
    private final FeedBackfillProperties backfillProperties;
    private final ActivityEventRepositoryPort activityRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final ReviewRepositoryPort reviewRepository;
    private final UserBookStatusRepositoryPort statusRepository;

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        userService.getUserById(followerId);
        userService.getUserById(followingId);

        if (!followRepository.existsByFollowerAndFollowing(followerId, followingId)) {
            Follow follow = Follow.builder()
                    .followerId(followerId)
                    .followingId(followingId)
                    .build();
            followRepository.save(follow);
            applicationEventPublisher.publishEvent(new FollowCreatedEvent(this, followerId, followingId));
        }

        backfillRecentActivities(followerId, followingId);
    }

    @Transactional
    public void unfollow(Long followerId, Long followingId) {
        followRepository.delete(followerId, followingId);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followRepository.existsByFollowerAndFollowing(followerId, followingId);
    }

    public FollowStats getStats(Long userId) {
        return FollowStats.builder()
                .followers(followRepository.countFollowers(userId))
                .following(followRepository.countFollowing(userId))
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserSearchItemDto> getFollowers(Long userId) {
        return followRepository.findFollowers(userId).stream()
                .map(Follow::getFollowerId)
                .map(userService::getUserById)
                .map(user -> UserSearchItemDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .following(false)
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSearchItemDto> getFollowing(Long userId) {
        return followRepository.findFollowing(userId).stream()
                .map(Follow::getFollowingId)
                .map(userService::getUserById)
                .map(user -> UserSearchItemDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .following(true)
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserSearchItemDto> searchUsers(Long currentUserId, String query, int limit) {
        String searchQuery = query == null ? "" : query.trim();
        if (searchQuery.isBlank()) {
            return List.of();
        }

        return userService.searchByUsername(searchQuery, limit).stream()
                .filter(user -> !user.getId().equals(currentUserId))
                .map(user -> UserSearchItemDto.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .avatarUrl(user.getAvatarUrl())
                        .following(followRepository.existsByFollowerAndFollowing(currentUserId, user.getId()))
                        .build())
                .collect(Collectors.toList());
    }

    private void backfillRecentActivities(Long followerId, Long followingId) {
        LocalDateTime since = LocalDateTime.now().minusDays(backfillProperties.getDays());
        int limit = backfillProperties.getLimit();

        List<Review> reviews = reviewRepository.findRecentByUserId(followingId, since, limit);
        List<UserBookStatus> statuses = statusRepository.findRecentByUserId(followingId, since, limit);

        List<ActivityEvent> candidates = new ArrayList<>();
        for (Review review : reviews) {
            candidates.add(ActivityEvent.builder()
                    .actorId(followingId)
                    .targetUserId(followerId)
                    .type(ActivityType.REVIEWED)
                    .bookId(review.getBookId())
                    .reviewId(review.getId())
                    .additionalData("{\"rating\": " + review.getRating() + "}")
                    .createdAt(review.getCreatedAt())
                    .build());
        }
        for (UserBookStatus status : statuses) {
            ActivityType type = mapStatusToActivityType(status.getStatus());
            if (type == null) {
                continue;
            }
            candidates.add(ActivityEvent.builder()
                    .actorId(followingId)
                    .targetUserId(followerId)
                    .type(type)
                    .bookId(status.getBookId())
                    .createdAt(status.getUpdatedAt())
                    .build());
        }

        candidates.sort(Comparator.comparing(ActivityEvent::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));

        int saved = 0;
        for (ActivityEvent candidate : candidates) {
            if (saved >= limit) {
                break;
            }
            if (candidate.getCreatedAt() == null) {
                continue;
            }
            if (activityRepository.existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
                    candidate.getActorId(),
                    candidate.getTargetUserId(),
                    candidate.getBookId(),
                    candidate.getType(),
                    candidate.getCreatedAt())) {
                continue;
            }
            activityRepository.save(candidate);
            saved++;
        }
    }

    private ActivityType mapStatusToActivityType(ReadingStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case WANT_TO_READ -> ActivityType.WANT_TO_READ;
            case READING -> ActivityType.STARTED_READING;
            case READ -> ActivityType.FINISHED_READING;
            case ABANDONED -> ActivityType.ABANDONED;
        };
    }
}
