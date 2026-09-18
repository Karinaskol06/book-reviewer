package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.infrastructure.elasticsearch.document.ReviewDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;
    @Mock
    private BookRepositoryPort bookRepository;
    @Mock
    private UserBookStatusRepositoryPort bookStatusRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void clampLimit_capsAtTwentyAndFloorsAtOne() {
        // If sth asks for 100 recs, service only allows for max 20
        assertThat(recommendationService.clampLimit(100)).isEqualTo(20);
        assertThat(recommendationService.clampLimit(0)).isEqualTo(1);
        assertThat(recommendationService.clampLimit(6)).isEqualTo(6);
    }

    @Test
    void buildExcludedBookIds_includesShelfAndReviewedBooks() {
        when(bookStatusRepository.findByUserId(1L)).thenReturn(List.of(
                UserBookStatus.builder().userId(1L).bookId(10L).build(),
                UserBookStatus.builder().userId(1L).bookId(11L).build()
        ));
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(
                        Review.builder().userId(1L).bookId(11L).rating(5).build(),
                        Review.builder().userId(1L).bookId(12L).rating(3).build()
                ))
        );

        Set<Long> excludedBooks = recommendationService.buildExcludedBookIds(1L);

        assertThat(excludedBooks).containsExactlyInAnyOrder(10L, 11L, 12L);
    }

    @Test
    void postgresFallback_prefersShelfGenresAndSkipsExcluded() {
        Set<Long> excluded = Set.of(1L, 2L);
        Book shelfBook = Book.builder().id(1L).title("Shelf Book 1").genres(Set.of("Fantasy")).build();
        Book candidate = Book.builder().id(6L).title("Book to rec").author("Author")
                .genres(Set.of("Fantasy")).build();
        BookResponse candidateResponse = BookResponse.builder().id(6L).title("Book to rec").author("Author")
                .genres(Set.of("Fantasy")).build();

        when(bookStatusRepository.findByUserId(1L)).thenReturn(List.of(
                UserBookStatus.builder().userId(1L).bookId(1L).build()
        ));
        when(bookRepository.findById(1L)).thenReturn(Optional.of(shelfBook));
        when(bookRepository.findByGenre(eq("Fantasy"), eq(0), anyInt()))
                .thenReturn(List.of(shelfBook, candidate));
        when(bookMapper.toResponse(candidate)).thenReturn(candidateResponse);

        List<BookResponse> result = recommendationService.fromPostgresFallback(1L, 6, excluded);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(6L);
        assertThat(result.getFirst().getRecommendationReason()).isEqualTo("Because you enjoy Fantasy");
    }

    @Test
    void postgresFallback_usesReviewGenresWhenShelfHasNoGenres() {
        Set<Long> excluded = Set.of(9L);
        Book reviewed = Book.builder().id(9L).title("Test title").genres(Set.of("Mystery")).build();
        Book candidate = Book.builder().id(90L).title("Another title").genres(Set.of("Mystery")).build();
        BookResponse response = BookResponse.builder().id(90L).title("Another title").build();

        when(bookStatusRepository.findByUserId(1L)).thenReturn(List.of());
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(
                new PageImpl<>(List.of(Review.builder().userId(1L).bookId(9L).rating(5).build()))
        );
        when(bookRepository.findById(9L)).thenReturn(Optional.of(reviewed));
        when(bookRepository.findByGenre(eq("Mystery"), eq(0), anyInt())).thenReturn(List.of(candidate));
        when(bookMapper.toResponse(candidate)).thenReturn(response);

        List<BookResponse> result = recommendationService.fromPostgresFallback(1L, 6, excluded);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(90L);
        assertThat(result.getFirst().getRecommendationReason()).contains("Mystery");
    }

    @Test
    void postgresFallback_usesTrendingWhenNoTasteSignal() {
        Set<Long> excluded = Set.of();
        Book trending = Book.builder().id(70L).title("Hot").author("B").build();
        BookResponse trendingResponse = BookResponse.builder().id(70L).title("Hot").build();

        when(bookStatusRepository.findByUserId(1L)).thenReturn(List.of());
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        when(bookRepository.findTrending(6, null)).thenReturn(List.of(trending));
        when(bookMapper.toResponse(trending)).thenReturn(trendingResponse);

        List<BookResponse> result = recommendationService.fromPostgresFallback(1L, 6, excluded);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getRecommendationReason()).isEqualTo("Trending in the archive");
    }

    @Test
    void getRecommendations_fallsBackWhenElasticsearchFails() {
        Book trending = Book.builder().id(70L).title("Hot").build();
        BookResponse trendingResponse = BookResponse.builder().id(70L).title("Hot").build();

        when(bookStatusRepository.findByUserId(1L)).thenReturn(List.of());
        when(reviewRepository.findByUserId(eq(1L), any(Pageable.class))).thenReturn(Page.empty());
        when(elasticsearchOperations.search(any(Query.class), eq(ReviewDocument.class)))
                .thenThrow(new RuntimeException("ES down"));
        when(bookRepository.findTrending(6, null)).thenReturn(List.of(trending));
        when(bookMapper.toResponse(trending)).thenReturn(trendingResponse);

        List<BookResponse> result = recommendationService.getRecommendations(1L, 6);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getId()).isEqualTo(70L);
    }
}
