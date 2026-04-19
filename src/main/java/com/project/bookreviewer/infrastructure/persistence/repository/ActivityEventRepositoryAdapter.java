package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ActivityEvent;
import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.port.outbound.ActivityEventRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ActivityEventEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ActivityEventRepositoryAdapter implements ActivityEventRepositoryPort {
    private final JpaActivityEventRepository jpaRepo;

    @Override
    public ActivityEvent save(ActivityEvent event) {
        ActivityEventEntity entity = ActivityEventEntity.builder()
                .actorId(event.getActorId())
                .targetUserId(event.getTargetUserId())
                .type(event.getType())
                .bookId(event.getBookId())
                .reviewId(event.getReviewId())
                .additionalData(event.getAdditionalData())
                .build();
        ActivityEventEntity saved = jpaRepo.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Page<ActivityEvent> findByTargetUserId(Long targetUserId, Pageable pageable) {
        return jpaRepo.findByTargetUserIdOrderByCreatedAtDesc(targetUserId, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Page<ActivityEvent> findByTargetUserIdIn(List<Long> targetUserIds, Pageable pageable) {
        return jpaRepo.findByTargetUserIdInOrderByCreatedAtDesc(targetUserIds, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Page<ActivityEvent> findFeedEvents(Long userId, List<Long> targetUserIds, Pageable pageable) {
        return jpaRepo.findFeedEvents(userId, targetUserIds, pageable).map(this::mapToDomain);
    }

    @Override
    public List<ActivityEvent> findRecentSelfActivities(Long userId, int limit, LocalDateTime since) {
        Pageable pageable = PageRequest.of(0, limit);
        return jpaRepo.findRecentSelfActivities(userId, since, pageable)
                .stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
            Long actorId, Long targetUserId, Long bookId, ActivityType type, LocalDateTime createdAt) {
        return jpaRepo.existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
                actorId, targetUserId, bookId, type, createdAt);
    }

    private ActivityEvent mapToDomain(ActivityEventEntity entity) {
        return ActivityEvent.builder()
                .id(entity.getId())
                .actorId(entity.getActorId())
                .targetUserId(entity.getTargetUserId())
                .type(entity.getType())
                .bookId(entity.getBookId())
                .reviewId(entity.getReviewId())
                .additionalData(entity.getAdditionalData())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}