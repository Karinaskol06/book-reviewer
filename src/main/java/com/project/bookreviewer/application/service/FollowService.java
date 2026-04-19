package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.FollowStats;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.config.FeedBackfillProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FollowService {
    private final FollowRepositoryPort followRepository;
    private final UserService userService;
    private final FeedBackfillProperties backfillProperties;
    private final ActivityEventRepositoryPort activityRepository;

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