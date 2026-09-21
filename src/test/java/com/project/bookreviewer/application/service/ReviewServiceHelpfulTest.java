package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.exception.UnauthorizedException;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.ReviewHelpful;
import com.project.bookreviewer.domain.port.outbound.ReviewHelpfulRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceHelpfulTest {

    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private ReviewHelpfulRepositoryPort reviewHelpfulRepository;
    @Mock
    private BookService bookService;
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void toggleHelpful_addsVoteAndIncrementsCount() {
        Review review = Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(5)
                .verdict("Great")
                .helpfulCount(2)
                .build();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewHelpfulRepository.existsByReviewIdAndUserId(10L, 99L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.toggleHelpful(10L, 99L);

        ArgumentCaptor<ReviewHelpful> voteCaptor = ArgumentCaptor.forClass(ReviewHelpful.class);
        verify(reviewHelpfulRepository).save(voteCaptor.capture());
        assertThat(voteCaptor.getValue().getReviewId()).isEqualTo(10L);
        assertThat(voteCaptor.getValue().getUserId()).isEqualTo(99L);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getHelpfulCount()).isEqualTo(3);
    }

    @Test
    void toggleHelpful_removesExistingVoteAndDecrementsCount() {
        Review review = Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(5)
                .verdict("Great")
                .helpfulCount(2)
                .build();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));
        when(reviewHelpfulRepository.existsByReviewIdAndUserId(10L, 99L)).thenReturn(true);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.toggleHelpful(10L, 99L);

        verify(reviewHelpfulRepository).deleteByReviewIdAndUserId(10L, 99L);
        verify(reviewHelpfulRepository, never()).save(any());

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        assertThat(reviewCaptor.getValue().getHelpfulCount()).isEqualTo(1);
    }

    @Test
    void toggleHelpful_rejectsOwnReview() {
        Review review = Review.builder()
                .id(10L)
                .userId(99L)
                .bookId(5L)
                .rating(4)
                .verdict("Mine")
                .helpfulCount(0)
                .build();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.toggleHelpful(10L, 99L))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("own review");

        verify(reviewHelpfulRepository, never()).save(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void toggleHelpful_missingReview_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.toggleHelpful(404L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
