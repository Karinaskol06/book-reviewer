package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(BookRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:bookfilter;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class BookRepositoryAdapterFilterTest {

    @Autowired
    private BookRepositoryAdapter bookRepositoryAdapter;
    @Autowired
    private JpaBookRepository jpaBookRepository;
    @Autowired
    private JpaReviewRepository jpaReviewRepository;

    private Long fastBookId;
    private Long slowBookId;
    private Long warnedBookId;
    private Long safeBookId;

    @BeforeEach
    void setUp() {
        jpaReviewRepository.deleteAll();
        jpaBookRepository.deleteAll();

        BookEntity fastHigh = saveBook("Fast Pace", "Author A", 4.5);
        BookEntity slowHigh = saveBook("Slow Pace", "Author B", 4.8);
        BookEntity warned = saveBook("Warned Book", "Author C", 4.2);
        BookEntity safe = saveBook("Safe Book", "Author D", 4.1);

        fastBookId = fastHigh.getId();
        slowBookId = slowHigh.getId();
        warnedBookId = warned.getId();
        safeBookId = safe.getId();

        // Dominant pacing FAST (2 FAST vs 1 SLOW)
        saveReview(fastBookId, 1L, Pacing.FAST, Set.of());
        saveReview(fastBookId, 2L, Pacing.FAST, Set.of());
        saveReview(fastBookId, 3L, Pacing.SLOW, Set.of());

        // Dominant pacing SLOW
        saveReview(slowBookId, 1L, Pacing.SLOW, Set.of());
        saveReview(slowBookId, 2L, Pacing.SLOW, Set.of());
        saveReview(slowBookId, 3L, Pacing.FAST, Set.of());

        // Content warnings present
        saveReview(warnedBookId, 1L, Pacing.MEDIUM, Set.of("violence"));
        // No content warnings
        saveReview(safeBookId, 1L, Pacing.MEDIUM, Set.of());
    }

    @Test
    void filterBooks_withPacing_keepsOnlyDominantPacingMatches() {
        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .pacing(Set.of(Pacing.FAST))
                .build();

        Page<Book> page = bookRepositoryAdapter.filterBooks(criteria, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Book::getId)
                .containsExactly(fastBookId);
    }

    @Test
    void filterBooks_withContentSafe_excludesBooksWithWarnings() {
        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .contentSafe(true)
                .build();

        Page<Book> page = bookRepositoryAdapter.filterBooks(criteria, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Book::getId)
                .contains(fastBookId, slowBookId, safeBookId)
                .doesNotContain(warnedBookId);
    }

    @Test
    void filterBooks_withMinRatingAndPacingAndContentSafe_appliesAll() {
        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .minRating(4)
                .pacing(Set.of(Pacing.FAST))
                .contentSafe(true)
                .build();

        Page<Book> page = bookRepositoryAdapter.filterBooks(criteria, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Book::getId)
                .containsExactly(fastBookId);
    }

    @Test
    void filterBooks_withGenre_matchesAliasesThatShareGenreKey() {
        BookEntity sciFi = saveBook("Dune", "Herbert", 4.5, Set.of("Sci Fi"));
        BookEntity romance = saveBook("Love Story", "Author", 4.0, Set.of("Romance"));

        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .genres(Set.of("Sci-Fi"))
                .build();

        Page<Book> page = bookRepositoryAdapter.filterBooks(criteria, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(Book::getId)
                .containsExactly(sciFi.getId())
                .doesNotContain(romance.getId());
    }

    private BookEntity saveBook(String title, String author, double averageRating) {
        return saveBook(title, author, averageRating, Set.of("Fiction"));
    }

    private BookEntity saveBook(String title, String author, double averageRating, Set<String> genres) {
        return jpaBookRepository.save(BookEntity.builder()
                .title(title)
                .normalizedTitle(title.toLowerCase())
                .author(author)
                .normalizedAuthor(author.toLowerCase())
                .description("desc")
                .publicationYear(2020)
                .genres(genres)
                .averageRating(averageRating)
                .ratingCount(3)
                .totalReviews(3)
                .build());
    }

    private void saveReview(Long bookId, Long userId, Pacing pacing, Set<String> warnings) {
        jpaReviewRepository.save(ReviewEntity.builder()
                .userId(userId)
                .bookId(bookId)
                .rating(5)
                .verdict("Good")
                .pacing(pacing)
                .contentWarnings(warnings)
                .build());
    }
}
