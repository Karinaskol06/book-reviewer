package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface ActivityEventRepositoryPort {
    ActivityEvent save(ActivityEvent event);
    Page<ActivityEvent> findByTargetUserId(Long targetUserId, Pageable pageable);
    // For feed of followed users
    Page<ActivityEvent> findByTargetUserIdIn(List<Long> targetUserIds, Pageable pageable);

    Page<ActivityEvent> findFeedEvents(Long userId, List<Long> targetUserIds, Pageable pageable);

    List<ActivityEvent> findRecentSelfActivities(Long userId, int limit, LocalDateTime since);

    boolean existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
            Long actorId, Long targetUserId, Long bookId, ActivityType type, LocalDateTime createdAt);
}