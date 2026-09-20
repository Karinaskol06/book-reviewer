package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.ReviewHelpful;

import java.util.Optional;

public interface ReviewHelpfulRepositoryPort {
    ReviewHelpful save(ReviewHelpful helpful);
    Optional<ReviewHelpful> findByReviewIdAndUserId(Long reviewId, Long userId);
    void deleteByReviewIdAndUserId(Long reviewId, Long userId);
    boolean existsByReviewIdAndUserId(Long reviewId, Long userId);
    void deleteByReviewId(Long reviewId);
}
