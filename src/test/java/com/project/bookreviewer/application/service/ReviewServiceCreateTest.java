package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreateReviewRequest;
import com.project.bookreviewer.domain.event.ReviewCreatedEvent;
import com.project.bookreviewer.domain.exception.DuplicateReviewException;
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

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceCreateTest {

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
    void createReview_savesPublishesCreatedEvent_andRefreshesBookStats() {
        when(reviewRepository.existsByUserIdAndBookId(1L, 5L)).thenReturn(false);
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review incoming = inv.getArgument(0);
            return Review.builder()
                    .id(99L)
                    .userId(incoming.getUserId())
                    .bookId(incoming.getBookId())
                    .rating(incoming.getRating())
                    .verdict(incoming.getVerdict())
                    .detailedReview(incoming.getDetailedReview())
                    .pacing(incoming.getPacing())
                    .mood(incoming.getMood())
                    .whoIsItFor(incoming.getWhoIsItFor())
                    .whoIsItNotFor(incoming.getWhoIsItNotFor())
                    .contentWarnings(incoming.getContentWarnings())
                    .spoilerContent(incoming.getSpoilerContent())
                    .hasSpoiler(incoming.getHasSpoiler())
                    .tags(incoming.getTags())
                    .helpfulCount(incoming.getHelpfulCount())
                    .build();
        });

        CreateReviewRequest request = baseRequest();
        request.setSpoilerContent("twist ending");

        Review saved = reviewService.createReview(1L, 5L, request);

        ArgumentCaptor<Review> reviewCaptor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(reviewCaptor.capture());
        Review persisted = reviewCaptor.getValue();
        assertThat(persisted.getUserId()).isEqualTo(1L);
        assertThat(persisted.getBookId()).isEqualTo(5L);
        assertThat(persisted.getRating()).isEqualTo(4);
        assertThat(persisted.getVerdict()).isEqualTo("Solid read");
        assertThat(persisted.getHelpfulCount()).isZero();
        assertThat(persisted.getHasSpoiler()).isTrue();
        assertThat(persisted.getId()).isNull();

        assertThat(saved.getId()).isEqualTo(99L);
        verify(applicationEventPublisher).publishEvent(org.mockito.ArgumentMatchers.argThat(event ->
                event instanceof ReviewCreatedEvent created
                        && created.getReview().getId().equals(99L)
                        && created.getReview().getBookId().equals(5L)
        ));
        verify(bookService).updateBookRatingStats(5L);
    }

    @Test
    void createReview_duplicateForSameUserAndBook_throws() {
        when(reviewRepository.existsByUserIdAndBookId(1L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(1L, 5L, baseRequest()))
                .isInstanceOf(DuplicateReviewException.class)
                .hasMessageContaining("already reviewed");

        verify(reviewRepository, never()).save(any());
        verify(applicationEventPublisher, never()).publishEvent(any());
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
