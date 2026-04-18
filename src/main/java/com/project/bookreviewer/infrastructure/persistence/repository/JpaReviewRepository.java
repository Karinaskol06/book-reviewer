package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    @Query("SELECT r.pacing, COUNT(r) FROM ReviewEntity r WHERE r.bookId = :bookId GROUP BY r.pacing ORDER BY COUNT(r) DESC")
    List<Object[]> findPacingCountsByBookId(@Param("bookId") Long bookId);

    @Query("SELECT DISTINCT m FROM ReviewEntity r JOIN r.mood m WHERE r.bookId = :bookId")
    List<String> findAllMoodsByBookId(@Param("bookId") Long bookId);

    @Query("SELECT COUNT(r) > 0 FROM ReviewEntity r WHERE r.bookId = :bookId AND SIZE(r.contentWarnings) > 0")
    boolean existsContentWarningsByBookId(@Param("bookId") Long bookId);
}
