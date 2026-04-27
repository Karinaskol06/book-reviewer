package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreateReviewRequest;
import com.project.bookreviewer.application.dto.response.RatingStatsDto;
import com.project.bookreviewer.application.dto.response.ReviewSnippetDto;
import com.project.bookreviewer.domain.event.ReviewCreatedEvent;
import com.project.bookreviewer.domain.exception.DuplicateReviewException;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewRepositoryPort reviewRepository;
    private final BookService bookService; // to update book rating cache
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public Review createReview(Long userId, Long bookId, CreateReviewRequest request) {
        if (reviewRepository.existsByUserIdAndBookId(userId, bookId)) {
            throw new DuplicateReviewException("You have already reviewed this book");
        }

        Review review = Review.builder()
                .userId(userId)
                .bookId(bookId)
                .rating(request.getRating())
                .verdict(request.getVerdict())
                .detailedReview(request.getDetailedReview())
                .pacing(request.getPacing())
                .mood(request.getMood())
                .whoIsItFor(request.getWhoIsItFor())
                .whoIsItNotFor(request.getWhoIsItNotFor())
                .contentWarnings(request.getContentWarnings())
                .spoilerContent(request.getSpoilerContent())
                .hasSpoiler(request.getSpoilerContent() != null && !request.getSpoilerContent().isBlank())
                .tags(request.getTags())
                .helpfulCount(0)
                .build();

        Review saved = reviewRepository.save(review);
        applicationEventPublisher.publishEvent(new ReviewCreatedEvent(this, saved));
        bookService.updateBookRatingStats(bookId); // recalc and cache
        return saved;
    }

    @Transactional
    @PreAuthorize("@reviewAuthorization.isReviewOwner(#reviewId, authentication.name)")
    public Review updateReview(Long reviewId, CreateReviewRequest request) {
        Review existing = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));

        Review updated = Review.builder()
                .id(reviewId)
                .userId(existing.getUserId())
                .bookId(existing.getBookId())
                .rating(request.getRating())
                .verdict(request.getVerdict())
                .detailedReview(request.getDetailedReview())
                .pacing(request.getPacing())
                .mood(request.getMood())
                .whoIsItFor(request.getWhoIsItFor())
                .whoIsItNotFor(request.getWhoIsItNotFor())
                .contentWarnings(request.getContentWarnings())
                .spoilerContent(request.getSpoilerContent())
                .hasSpoiler(request.getSpoilerContent() != null && !request.getSpoilerContent().isBlank())
                .tags(request.getTags())
                .helpfulCount(existing.getHelpfulCount())
                .createdAt(existing.getCreatedAt())
                .build();

        Review saved = reviewRepository.save(updated);
        bookService.updateBookRatingStats(existing.getBookId());
        return saved;
    }

    @Transactional
    @PreAuthorize("@reviewAuthorization.isReviewOwner(#reviewId, authentication.name) or hasRole('ADMIN')")
    public void deleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        reviewRepository.deleteById(reviewId);
        bookService.updateBookRatingStats(review.getBookId());
    }

    public RatingStatsDto getRatingStatsDto(Long bookId) {
        Object[] stats = reviewRepository.getRatingStats(bookId);
        if (stats == null || stats.length < 7) {
            return RatingStatsDto.builder()
                    .average(0.0)
                    .total(0)
                    .distribution(Map.of(5,0,4,0,3,0,2,0,1,0))
                    .build();
        }

        Double avg = extractDouble(stats[0]);
        Long total = extractLong(stats[1]);

        Map<Integer, Integer> distribution = Map.of(
                5, extractLong(stats[2]).intValue(),
                4, extractLong(stats[3]).intValue(),
                3, extractLong(stats[4]).intValue(),
                2, extractLong(stats[5]).intValue(),
                1, extractLong(stats[6]).intValue()
        );

        return RatingStatsDto.builder()
                .average(avg != null ? avg : 0.0)
                .total(total != null ? total.intValue() : 0)
                .distribution(distribution)
                .build();
    }

    public ReviewSnippetDto getReviewSnippet(Long reviewId) {
        return reviewRepository.findById(reviewId)
                .map(review -> ReviewSnippetDto.builder()
                        .id(review.getId())
                        .verdict(review.getVerdict())
                        .rating(review.getRating())
                        .detailedReview(review.getDetailedReview())
                        .whoIsItFor(review.getWhoIsItFor())
                        .mood(review.getMood())
                        .helpfulCount(review.getHelpfulCount())
                        .build())
                .orElse(null);
    }

    private Double extractDouble(Object value) {
        if (value instanceof Object[] nested && nested.length > 0) {
            return extractDouble(nested[0]);
        }

        return switch (value) {
            case null -> null;
            case Double v -> v;
            case Number number -> number.doubleValue();
            default -> throw new IllegalArgumentException("Cannot convert to Double: " + value.getClass());
        };
    }

    private Long extractLong(Object value) {
        if (value instanceof Object[] nested && nested.length > 0) {
            return extractLong(nested[0]);
        }

        return switch (value) {
            case null -> 0L;
            case Long l -> l;
            case Number number -> number.longValue();
            default -> throw new IllegalArgumentException("Cannot convert to Long: " + value.getClass());
        };
    }

    @Transactional(readOnly = true)
    public Page<Review> getReviewsByBook(Long bookId, Pageable pageable) {
        return reviewRepository.findByBookId(bookId, pageable);
    }

    public Object[] getRatingStats(Long bookId) {
        return reviewRepository.getRatingStats(bookId);
    }

    public boolean hasUserReviewed(Long userId, Long bookId) {
        return reviewRepository.existsByUserIdAndBookId(userId, bookId);
    }

    public int countReviewsByUser(Long userId) {
        return (int) reviewRepository.countByUserId(userId);
    }

}
