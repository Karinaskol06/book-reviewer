package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.FollowStats;
import com.project.bookreviewer.application.dto.response.UserSearchItemDto;
import com.project.bookreviewer.domain.event.FollowCreatedEvent;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.config.FeedBackfillProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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

    @Transactional
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new IllegalArgumentException("Cannot follow yourself");
        }
        // Ensure both users exist
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
        List<ActivityEvent> recentActivities = activityRepository.findRecentSelfActivities(
                followingId, backfillProperties.getLimit(), since);

        for (ActivityEvent original : recentActivities) {
            if (!activityRepository.existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
                    original.getActorId(),          // actor = followed user
                    followerId,                     // target = the new follower
                    original.getBookId(),
                    original.getType(),
                    original.getCreatedAt())) {

                ActivityEvent cloned = ActivityEvent.builder()
                        .actorId(original.getActorId())
                        .targetUserId(followerId)   // now owned by follower
                        .type(original.getType())
                        .bookId(original.getBookId())
                        .reviewId(original.getReviewId())
                        .additionalData(original.getAdditionalData())
                        .createdAt(original.getCreatedAt())  // preserve original timestamp
                        .build();
                activityRepository.save(cloned);
            }
        }
    }
}