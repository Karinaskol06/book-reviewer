package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.event.ReviewCreatedEvent;
import com.project.bookreviewer.domain.event.StatusChangedEvent;
import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ActivityService {
    private final ActivityEventRepositoryPort activityRepository;
    private final FollowRepositoryPort followRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStatusChanged(StatusChangedEvent event) {
        ActivityType type = mapStatusToActivityType(event.getNewStatus());
        if (type == null) return;

        // Create activity for the actor's own feed
        ActivityEvent selfEvent = ActivityEvent.builder()
                .actorId(event.getUserId())
                .targetUserId(event.getUserId())
                .type(type)
                .bookId(event.getBookId())
                .build();
        activityRepository.save(selfEvent);

        // Also create events for all followers' feeds
        List<Follow> followers = followRepository.findFollowers(event.getUserId());
        for (Follow follow : followers) {
            ActivityEvent followerEvent = ActivityEvent.builder()
                    .actorId(event.getUserId())
                    .targetUserId(follow.getFollowerId())
                    .type(type)
                    .bookId(event.getBookId())
                    .build();
            activityRepository.save(followerEvent);
        }
        log.info("Status change events created for user {} and {} followers", event.getUserId(), followers.size());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        // Actor's own feed
        ActivityEvent selfEvent = ActivityEvent.builder()
                .actorId(event.getReview().getUserId())
                .targetUserId(event.getReview().getUserId())
                .type(ActivityType.REVIEWED)
                .bookId(event.getReview().getBookId())
                .reviewId(event.getReview().getId())
                .additionalData("{\"rating\": " + event.getReview().getRating() + "}")
                .build();
        activityRepository.save(selfEvent);

        // Followers' feeds
        List<Follow> followers = followRepository.findFollowers(event.getReview().getUserId());
        for (Follow follow : followers) {
            ActivityEvent followerEvent = ActivityEvent.builder()
                    .actorId(event.getReview().getUserId())
                    .targetUserId(follow.getFollowerId())
                    .type(ActivityType.REVIEWED)
                    .bookId(event.getReview().getBookId())
                    .reviewId(event.getReview().getId())
                    .additionalData("{\"rating\": " + event.getReview().getRating() + "}")
                    .build();
            activityRepository.save(followerEvent);
        }
        log.info("Review created events for user {} and {} followers", event.getReview().getUserId(), followers.size());
    }

    private ActivityType mapStatusToActivityType(ReadingStatus status) {
        return switch (status) {
            case WANT_TO_READ -> ActivityType.WANT_TO_READ;
            case READING -> ActivityType.STARTED_READING;
            case READ -> ActivityType.FINISHED_READING;
            case ABANDONED -> ActivityType.ABANDONED;
        };
    }
}