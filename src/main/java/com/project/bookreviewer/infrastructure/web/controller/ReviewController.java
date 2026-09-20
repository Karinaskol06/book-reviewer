package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.CreateReviewRequest;
import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.application.mapper.ReviewMapper;
import com.project.bookreviewer.application.service.ReviewService;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ReviewController {
    private final ReviewService reviewService;
    private final ReviewMapper reviewMapper;
    private final SecurityUtils securityUtils;

    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<Page<ReviewResponse>> getBookReviews(
            @PathVariable Long bookId,
            @PageableDefault(size = 10) Pageable pageable,
            @RequestParam(required = false, defaultValue = "false") boolean includeSpoilers) {
        Long currentUserId = securityUtils.getCurrentUserIdOrNull();
        Page<Review> reviews = reviewService.getReviewsByBook(bookId, pageable);

        Page<ReviewResponse> responsePage = reviews.map(review -> {
            ReviewResponse response = reviewMapper.toResponse(review, includeSpoilers);
            response.setHasHelpful(reviewService.hasUserMarkedHelpful(review.getId(), currentUserId));
            return response;
        });
        return ResponseEntity.ok(responsePage);
    }

    @PostMapping("/books/{bookId}/reviews")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long bookId,
            @Valid @RequestBody CreateReviewRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        Review review = reviewService.createReview(userId, bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewMapper.toResponse(review));
    }

    @PutMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody CreateReviewRequest request) {
        Review review = reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(reviewMapper.toResponse(review));
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reviews/{reviewId}/helpful")
    public ResponseEntity<Void> toggleHelpful(@PathVariable Long reviewId) {
        Long userId = securityUtils.getCurrentUserId();
        reviewService.toggleHelpful(reviewId, userId);
        return ResponseEntity.noContent().build();
    }

}