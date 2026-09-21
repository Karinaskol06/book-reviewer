package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReadingStatusEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.UserBookStatusEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(BookRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:booktrending;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class BookRepositoryAdapterTrendingTest {

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

        // classic: many old reviews, none recent
        for (long u = 1; u <= 5; u++) {
            saveReview(classic.getId(), u, LocalDateTime.now().minusDays(60));
        }
        // hot month: reviews in last 30d but not 7d
        saveReview(hotThisMonth.getId(), 1L, LocalDateTime.now().minusDays(14));
        saveReview(hotThisMonth.getId(), 2L, LocalDateTime.now().minusDays(20));
        // hot week: most 7d reviews (wins even with lower rating)
        saveReview(hotThisWeek.getId(), 1L, LocalDateTime.now().minusDays(1));
        saveReview(hotThisWeek.getId(), 2L, LocalDateTime.now().minusDays(2));
        saveReview(hotThisWeek.getId(), 3L, LocalDateTime.now().minusDays(3));
        // quiet gem: no reviews — only high averageRating on the book row
        // (should rank after books with review activity)

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
