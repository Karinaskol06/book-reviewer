package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.TasteProfileResponse;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TasteProfileServiceTest {

    @Mock
    private UserBookStatusRepositoryPort statusRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private BookRepositoryPort bookRepository;

    @InjectMocks
    private TasteProfileService tasteProfileService;

    @Test
    void emptyShelfAndNoReviews_returnsEmptyProfile() {
        when(statusRepository.findByUserId(1L)).thenReturn(List.of());
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TasteProfileResponse profile = tasteProfileService.getTasteProfile(1L);

        assertThat(profile.getTopGenres()).isEmpty();
        assertThat(profile.getTopMoods()).isEmpty();
        assertThat(profile.getDominantPacing()).isNull();
        assertThat(profile.getAverageRating()).isNull();
        assertThat(profile.getSampleSize()).isZero();
        assertThat(profile.getReviewSampleSize()).isZero();
    }

    @Test
    void abandonedOnly_excludesGenresFromTaste() {
        when(statusRepository.findByUserId(1L)).thenReturn(List.of(
                status(10L, ReadingStatus.ABANDONED)
        ));
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TasteProfileResponse profile = tasteProfileService.getTasteProfile(1L);

        assertThat(profile.getTopGenres()).isEmpty();
        assertThat(profile.getSampleSize()).isZero();
    }

    @Test
    void weightsReadAboveWantToRead_andBoostsHighRatedReviews() {
        when(statusRepository.findByUserId(1L)).thenReturn(List.of(
                status(10L, ReadingStatus.WANT_TO_READ),
                status(20L, ReadingStatus.READ)
        ));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book(10L, Set.of("Mystery"))));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(book(20L, Set.of("Fantasy"))));
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        review(100L, 20L, 5, Set.of("DARK"), Pacing.FAST)
                )));

        TasteProfileResponse profile = tasteProfileService.getTasteProfile(1L);

        assertThat(profile.getTopGenres()).extracting(TasteProfileResponse.GenreShare::getName)
                .containsExactly("Fantasy", "Mystery");
        assertThat(profile.getTopGenres().get(0).getWeight())
                .isGreaterThan(profile.getTopGenres().get(1).getWeight());
        assertThat(profile.getTopGenres()).allSatisfy(g -> assertThat(g.getSharePercent()).isPositive());
        assertThat(profile.getSampleSize()).isEqualTo(2);
    }

    @Test
    void topN_limitsGenresAndBreaksTiesAlphabetically() {
        when(statusRepository.findByUserId(1L)).thenReturn(List.of(
                status(1L, ReadingStatus.READ),
                status(2L, ReadingStatus.READ),
                status(3L, ReadingStatus.READ),
                status(4L, ReadingStatus.READ),
                status(5L, ReadingStatus.READ)
        ));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book(1L, Set.of("Zebra"))));
        when(bookRepository.findById(2L)).thenReturn(Optional.of(book(2L, Set.of("Alpha"))));
        when(bookRepository.findById(3L)).thenReturn(Optional.of(book(3L, Set.of("Beta"))));
        when(bookRepository.findById(4L)).thenReturn(Optional.of(book(4L, Set.of("Gamma"))));
        when(bookRepository.findById(5L)).thenReturn(Optional.of(book(5L, Set.of("Delta"))));
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        TasteProfileResponse profile = tasteProfileService.getTasteProfile(1L);

        // Equal READ weights → alphabetical, top 4
        assertThat(profile.getTopGenres()).extracting(TasteProfileResponse.GenreShare::getName)
                .containsExactly("Alpha", "Beta", "Delta", "Gamma");
    }

    @Test
    void noShelf_fallsBackToHighRatedReviewGenres() {
        when(statusRepository.findByUserId(1L)).thenReturn(List.of());
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        review(1L, 50L, 5, Set.of("HOPEFUL"), Pacing.MEDIUM),
                        review(2L, 51L, 2, Set.of("DARK"), Pacing.SLOW)
                )));
        when(bookRepository.findById(50L)).thenReturn(Optional.of(book(50L, Set.of("Literary"))));

        TasteProfileResponse profile = tasteProfileService.getTasteProfile(1L);

        assertThat(profile.getTopGenres()).extracting(TasteProfileResponse.GenreShare::getName)
                .containsExactly("Literary");
        assertThat(profile.getSampleSize()).isEqualTo(1);
    }

    @Test
    void fingerprint_includesMoodsDominantPacingAndAverageRating() {
        when(statusRepository.findByUserId(1L)).thenReturn(List.of(
                status(20L, ReadingStatus.READ)
        ));
        when(bookRepository.findById(20L)).thenReturn(Optional.of(book(20L, Set.of("Fantasy"))));
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(
                        review(1L, 20L, 5, Set.of("DARK", "HOPEFUL"), Pacing.FAST),
                        review(2L, 21L, 3, Set.of("DARK"), Pacing.FAST),
                        review(3L, 22L, 4, Set.of("WHIMSICAL"), Pacing.SLOW)
                )));

        TasteProfileResponse profile = tasteProfileService.getTasteProfile(1L);

        assertThat(profile.getTopMoods()).extracting(TasteProfileResponse.MoodCount::getName)
                .first().isEqualTo("DARK");
        assertThat(profile.getTopMoods().get(0).getCount()).isEqualTo(2);
        assertThat(profile.getDominantPacing()).isEqualTo("FAST");
        assertThat(profile.getAverageRating()).isEqualTo(4.0);
        assertThat(profile.getReviewSampleSize()).isEqualTo(3);
    }

    private static UserBookStatus status(Long bookId, ReadingStatus status) {
        return UserBookStatus.builder().userId(1L).bookId(bookId).status(status).build();
    }

    private static Book book(Long id, Set<String> genres) {
        return Book.builder().id(id).title("T").author("A").genres(genres).build();
    }

    private static Review review(Long id, Long bookId, int rating, Set<String> mood, Pacing pacing) {
        return Review.builder()
                .id(id)
                .userId(1L)
                .bookId(bookId)
                .rating(rating)
                .mood(mood)
                .pacing(pacing)
                .verdict("v")
                .build();
    }
}
