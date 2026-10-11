package com.project.bookreviewer.infrastructure.listener;

import com.project.bookreviewer.domain.event.BookCreatedEvent;
import com.project.bookreviewer.domain.event.FollowCreatedEvent;
import com.project.bookreviewer.domain.event.ReviewCreatedEvent;
import com.project.bookreviewer.domain.event.ReviewDeletedEvent;
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
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Infrastructure listener: reacts to plain domain events and writes activity-feed rows.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityEventListener {
    private final ActivityEventRepositoryPort activityRepository;
    private final FollowRepositoryPort followRepository;

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleStatusChanged(StatusChangedEvent event) {
        ActivityType type = mapStatusToActivityType(event.newStatus());
        if (type == null) {
            return;
        }

        List<Follow> followers = followRepository.findFollowers(event.userId());
        for (Follow follow : followers) {
            ActivityEvent followerEvent = ActivityEvent.builder()
                    .actorId(event.userId())
                    .targetUserId(follow.getFollowerId())
                    .type(type)
                    .bookId(event.bookId())
                    .build();
            activityRepository.save(followerEvent);
        }
        log.info("Status change events created for user {} and {} followers", event.userId(), followers.size());
    }

    @EventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleReviewCreated(ReviewCreatedEvent event) {
        List<Follow> followers = followRepository.findFollowers(event.review().getUserId());
        for (Follow follow : followers) {
            ActivityEvent followerEvent = ActivityEvent.builder()
                    .actorId(event.review().getUserId())
                    .targetUserId(follow.getFollowerId())
                    .type(ActivityType.REVIEWED)
                    .bookId(event.review().getBookId())
                    .reviewId(event.review().getId())
                    .additionalData("{\"rating\": " + event.review().getRating() + "}")
                    .createdAt(event.review().getCreatedAt())
                    .build();
            activityRepository.save(followerEvent);
        }
        log.info("Review created events for user {} and {} followers",
                event.review().getUserId(), followers.size());
    }

    @EventListener
    @Transactional
    public void handleReviewDeleted(ReviewDeletedEvent event) {
        activityRepository.deleteByReviewId(event.reviewId());
        log.info("Removed feed events for deleted review {}", event.reviewId());
    }

    @EventListener
    public void handleBookCreated(BookCreatedEvent event) {
        // Catalog adds are not fan-out to feeds for now (feed = reviews + shelf status only).
    }

    @EventListener
    public void handleFollowCreated(FollowCreatedEvent event) {
        // Follow relationships are not written as feed cards.
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
