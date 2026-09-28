package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReadingStatusEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.UserBookStatusEntity;
import com.project.bookreviewer.infrastructure.persistence.support.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Postgres coverage for native {@code findTrending} (INTERVAL windows, NULLS LAST, shelf exclusion).
 * Complements the faster H2 {@link BookRepositoryAdapterTrendingTest}.
 */
@DataJpaTest
@Testcontainers
@Import(BookRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookTrendingPostgresIT extends AbstractPostgresDataJpaTest {

    @Autowired
    private BookRepositoryAdapter bookRepositoryAdapter;
    @Autowired
    private JpaBookRepository jpaBookRepository;
    @Autowired
    private JpaReviewRepository jpaReviewRepository;
    @Autowired
    private JpaUserBookStatusRepository jpaUserBookStatusRepository;

    @BeforeEach
    void setUp() {
        jpaUserBookStatusRepository.deleteAll();
        jpaReviewRepository.deleteAll();
        jpaBookRepository.deleteAll();
    }

    @Test
    void findTrending_ranksByRecentReviewsThenRatingThenAllTime() {
        BookEntity hotThisWeek = saveBook("Hot Week", 3.0);
        BookEntity hotThisMonth = saveBook("Hot Month", 5.0);
        BookEntity classic = saveBook("Classic", 4.9);
        BookEntity highlyRatedQuiet = saveBook("Quiet Gem", 5.0);

        for (long u = 1; u <= 5; u++) {
            saveReview(classic.getId(), u, LocalDateTime.now().minusDays(60));
        }
        saveReview(hotThisMonth.getId(), 1L, LocalDateTime.now().minusDays(14));
        saveReview(hotThisMonth.getId(), 2L, LocalDateTime.now().minusDays(20));
        saveReview(hotThisWeek.getId(), 1L, LocalDateTime.now().minusDays(1));
        saveReview(hotThisWeek.getId(), 2L, LocalDateTime.now().minusDays(2));
        saveReview(hotThisWeek.getId(), 3L, LocalDateTime.now().minusDays(3));

        List<Book> trending = bookRepositoryAdapter.findTrending(10, null);

        assertThat(trending).extracting(Book::getId)
                .containsExactly(
                        hotThisWeek.getId(),
                        hotThisMonth.getId(),
                        highlyRatedQuiet.getId(),
                        classic.getId()
                );
    }

    @Test
    void findTrending_excludesBooksOnUserShelf() {
        BookEntity onShelf = saveBook("On Shelf", 4.0);
        BookEntity other = saveBook("Other", 4.0);
        saveReview(onShelf.getId(), 1L, LocalDateTime.now().minusDays(1));
        saveReview(onShelf.getId(), 2L, LocalDateTime.now().minusDays(1));
        saveReview(other.getId(), 1L, LocalDateTime.now().minusDays(1));

        jpaUserBookStatusRepository.save(UserBookStatusEntity.builder()
                .userId(42L)
                .bookId(onShelf.getId())
                .status(ReadingStatusEntity.READING)
                .build());

        List<Book> trending = bookRepositoryAdapter.findTrending(10, 42L);

        assertThat(trending).extracting(Book::getId)
                .containsExactly(other.getId())
                .doesNotContain(onShelf.getId());
    }

    private BookEntity saveBook(String title, double averageRating) {
        return jpaBookRepository.save(BookEntity.builder()
                .title(title)
                .normalizedTitle(title.toLowerCase())
                .author("Author")
                .normalizedAuthor("author " + title.toLowerCase())
                .description("desc")
                .publicationYear(2020)
                .genres(Set.of("Fiction"))
                .averageRating(averageRating)
                .ratingCount(0)
                .totalReviews(0)
                .build());
    }

    private void saveReview(Long bookId, Long userId, LocalDateTime createdAt) {
        ReviewEntity review = jpaReviewRepository.save(ReviewEntity.builder()
                .userId(userId)
                .bookId(bookId)
                .rating(5)
                .verdict("Good")
                .build());
        review.setCreatedAt(createdAt);
        review.setUpdatedAt(createdAt);
        jpaReviewRepository.save(review);
    }
}
