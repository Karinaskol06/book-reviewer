package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewRepositoryPort {
    Review save(Review review);
    Optional<Review> findById(Long id);
    Optional<Long> findUserIdByReviewId(Long reviewId);
    Page<Review> findByBookId(Long bookId, Pageable pageable);
    Optional<Review> findByUserIdAndBookId(Long userId, Long bookId);
    void deleteById(Long id);
    boolean existsByUserIdAndBookId(Long userId, Long bookId);
    long countByUserId(Long userId);
    // For rating stats
    Object[] getRatingStats(Long bookId); // returns [avg, count, distribution]
}
