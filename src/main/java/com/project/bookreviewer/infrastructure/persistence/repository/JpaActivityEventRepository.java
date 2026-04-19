package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.infrastructure.persistence.entity.ActivityEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JpaActivityEventRepository extends JpaRepository<ActivityEventEntity, Long> {
    Page<ActivityEventEntity> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId, Pageable pageable);

    @Query("SELECT e FROM ActivityEventEntity e WHERE e.targetUserId IN :userIds ORDER BY e.createdAt DESC")
    Page<ActivityEventEntity> findByTargetUserIdInOrderByCreatedAtDesc(@Param("userIds") List<Long> userIds,
                                                                       Pageable pageable);

    @Query("SELECT e FROM ActivityEventEntity e WHERE e.actorId = :userId AND e.targetUserId = :userId " +
            "AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<ActivityEventEntity> findRecentSelfActivities(@Param("userId") Long userId,
                                                       @Param("since") LocalDateTime since, Pageable pageable);

    boolean existsByActorIdAndTargetUserIdAndBookIdAndTypeAndCreatedAt(
            Long actorId, Long targetUserId, Long bookId, ActivityType type, LocalDateTime createdAt);


    @Query("SELECT e FROM ActivityEventEntity e WHERE " +
            "e.targetUserId = :userId OR " +
            "(e.targetUserId IN :followedIds AND e.actorId <> e.targetUserId) " +
            "ORDER BY e.createdAt DESC")
    Page<ActivityEventEntity> findFeedEvents(@Param("userId") Long userId,
                                             @Param("followedIds") List<Long> followedIds,
                                             Pageable pageable);
}