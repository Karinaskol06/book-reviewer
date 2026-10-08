package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.request.CreateReviewRequest;
import com.project.bookreviewer.application.dto.response.RatingStatsDto;
import com.project.bookreviewer.domain.model.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Driving port for review write/read flows used by HTTP adapters.
 */
public interface ReviewUseCase {
    Review createReview(Long userId, Long bookId, CreateReviewRequest request);

    Review updateReview(Long reviewId, CreateReviewRequest request);

    void deleteReview(Long reviewId);

    void toggleHelpful(Long reviewId, Long userId);

    boolean hasUserMarkedHelpful(Long reviewId, Long userId);

    Review getReview(Long reviewId);

    Page<Review> getReviewsByBook(Long bookId, Pageable pageable);

    Page<Review> getReviewsByUser(Long userId, Pageable pageable);

    RatingStatsDto getRatingStatsDto(Long bookId);

    boolean hasUserReviewed(Long userId, Long bookId);

    int countReviewsByUser(Long userId);
}
