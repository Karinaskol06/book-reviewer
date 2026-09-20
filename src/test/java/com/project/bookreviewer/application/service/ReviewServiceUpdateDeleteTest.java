package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreateReviewRequest;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.port.outbound.ReviewHelpfulRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceUpdateDeleteTest {

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
    void updateReview_preservesCreatedAtAndHelpfulCount_andRefreshesBookStats() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 1, 10, 12, 0);
        Review existing = Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(3)
                .verdict("Old")
                .spoilerContent("twist")
                .hasSpoiler(true)
                .helpfulCount(7)
                .createdAt(createdAt)
                .build();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReviewRequest request = baseRequest();
        request.setRating(5);
        request.setVerdict("New verdict");
        request.setSpoilerContent("new spoiler");

        Review saved = reviewService.updateReview(10L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review updated = captor.getValue();
        assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
        assertThat(updated.getHelpfulCount()).isEqualTo(7);
        assertThat(updated.getUserId()).isEqualTo(1L);
        assertThat(updated.getBookId()).isEqualTo(5L);
        assertThat(updated.getRating()).isEqualTo(5);
        assertThat(updated.getVerdict()).isEqualTo("New verdict");
        assertThat(saved.getCreatedAt()).isEqualTo(createdAt);
        verify(bookService).updateBookRatingStats(5L);
    }

    @Test
    void updateReview_clearsSpoilersWhenSpoilerContentBlank() {
        Review existing = Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(4)
                .verdict("Old")
                .spoilerContent("big twist")
                .hasSpoiler(true)
                .helpfulCount(1)
                .createdAt(LocalDateTime.of(2024, 2, 1, 9, 0))
                .build();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateReviewRequest request = baseRequest();
        request.setSpoilerContent(null);

        reviewService.updateReview(10L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getSpoilerContent()).isNull();
        assertThat(captor.getValue().getHasSpoiler()).isFalse();
    }

    @Test
    void updateReview_missingReview_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateReview(404L, baseRequest()))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewRepository, never()).save(any());
        verify(bookService, never()).updateBookRatingStats(any());
    }

    @Test
    void deleteReview_cascadesHelpfulVotes_andRefreshesBookStats() {
        Review existing = Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(4)
                .verdict("Gone")
                .build();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));

        reviewService.deleteReview(10L);

        verify(reviewHelpfulRepository).deleteByReviewId(10L);
        verify(reviewRepository).deleteById(10L);
        verify(bookService).updateBookRatingStats(5L);
    }

    @Test
    void deleteReview_missingReview_throws() {
        when(reviewRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteReview(404L))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(reviewHelpfulRepository, never()).deleteByReviewId(any());
        verify(reviewRepository, never()).deleteById(any());
        verify(bookService, never()).updateBookRatingStats(any());
    }

    private static CreateReviewRequest baseRequest() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(4);
        request.setVerdict("Solid read");
        request.setPacing(Pacing.MEDIUM);
        request.setMood(Set.of("HOPEFUL"));
        request.setWhoIsItFor("Fans of quiet stories");
        request.setWhoIsItNotFor("Readers wanting action");
        request.setTags(Set.of("HOPEFUL"));
        return request;
    }
}
