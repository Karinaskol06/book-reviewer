package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ReviewHelpfulEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface JpaReviewHelpfulRepository extends JpaRepository<ReviewHelpfulEntity, Long> {
    Optional<ReviewHelpfulEntity> findByReviewIdAndUserId(Long reviewId, Long userId);

    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);

    @Modifying
    @Query("DELETE FROM ReviewHelpfulEntity h WHERE h.reviewId = :reviewId AND h.userId = :userId")
    void deleteByReviewIdAndUserId(@Param("reviewId") Long reviewId, @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM ReviewHelpfulEntity h WHERE h.reviewId = :reviewId")
    void deleteByReviewId(@Param("reviewId") Long reviewId);
}
