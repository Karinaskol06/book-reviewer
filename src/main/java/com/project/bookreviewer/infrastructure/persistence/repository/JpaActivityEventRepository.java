package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.infrastructure.persistence.entity.ActivityEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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

    boolean existsByReviewIdAndTargetUserId(Long reviewId, Long targetUserId);

    boolean existsByActorIdAndTargetUserIdAndBookIdAndType(
            Long actorId, Long targetUserId, Long bookId, ActivityType type);

    @Query("SELECT e FROM ActivityEventEntity e WHERE " +
            "e.targetUserId = :userId AND e.actorId <> :userId " +
            "AND EXISTS (SELECT 1 FROM FollowEntity f WHERE f.followerId = :userId AND f.followingId = e.actorId) " +
            "ORDER BY e.createdAt DESC")
    Page<ActivityEventEntity> findFeedEvents(@Param("userId") Long userId, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ActivityEventEntity e WHERE e.reviewId = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("DELETE FROM ActivityEventEntity e WHERE e.actorId = :actorId AND e.targetUserId = :targetUserId")
    void deleteByActorIdAndTargetUserId(@Param("actorId") Long actorId, @Param("targetUserId") Long targetUserId);
}