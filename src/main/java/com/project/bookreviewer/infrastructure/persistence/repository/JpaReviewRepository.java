package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface JpaReviewRepository extends JpaRepository<ReviewEntity, Long> {
    Page<ReviewEntity> findByBookId(Long bookId, Pageable pageable);
    Optional<ReviewEntity> findByUserIdAndBookId(Long userId, Long bookId);
    @Query("SELECT r.userId FROM ReviewEntity r WHERE r.id = :reviewId")
    Optional<Long> findUserIdByReviewId(@Param("reviewId") Long reviewId);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);

    @Query("SELECT AVG(r.rating), COUNT(r), " +
            "SUM(CASE WHEN r.rating = 5 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.rating = 4 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.rating = 3 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.rating = 2 THEN 1 ELSE 0 END), " +
            "SUM(CASE WHEN r.rating = 1 THEN 1 ELSE 0 END) " +
            "FROM ReviewEntity r WHERE r.bookId = :bookId")
    Object[] getRatingStats(@Param("bookId") Long bookId);

    long countByUserId(Long userId);
}
