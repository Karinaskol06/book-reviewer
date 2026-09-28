package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.model.Pacing;
import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import com.project.bookreviewer.infrastructure.persistence.support.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Postgres coverage for book filter Specifications and search JPQL
 * (genre element-collection joins, DISTINCT, paging, countSearch).
 * Complements the faster H2 {@link BookRepositoryAdapterFilterTest}.
 */
@DataJpaTest
@Testcontainers
@Import(BookRepositoryAdapter.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookFilterSearchPostgresIT extends AbstractPostgresDataJpaTest {

    @Autowired
    private BookRepositoryAdapter bookRepositoryAdapter;
    @Autowired
    private JpaBookRepository jpaBookRepository;
    @Autowired
    private JpaReviewRepository jpaReviewRepository;

    @BeforeEach
    void setUp() {
        jpaReviewRepository.deleteAll();
        jpaBookRepository.deleteAll();
    }

    @Test
    void filterBooks_withGenre_joinsElementCollectionAndMatchesAliases() {
        BookEntity sciFi = saveBook("Dune", "Herbert", 4.5, Set.of("Sci Fi"));
        BookEntity romance = saveBook("Love Story", "Author", 4.0, Set.of("Romance"));
        saveBook("Other", "Someone", 3.5, Set.of("Fiction"));

        Page<Book> page = bookRepositoryAdapter.filterBooks(
                BookFilterCriteria.builder().genres(Set.of("Sci-Fi")).build(),
                PageRequest.of(0, 20)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Book::getId)
                .containsExactly(sciFi.getId())
                .doesNotContain(romance.getId());
    }

    @Test
    void filterBooks_withPacingAndContentSafe_pagesDistinctResults() {
        BookEntity fastSafe = saveBook("Fast Safe", "A", 4.5, Set.of("Fiction"));
        BookEntity fastWarned = saveBook("Fast Warned", "B", 4.6, Set.of("Fiction"));
        BookEntity slowSafe = saveBook("Slow Safe", "C", 4.7, Set.of("Fiction"));

        saveReview(fastSafe.getId(), 1L, Pacing.FAST, Set.of());
        saveReview(fastSafe.getId(), 2L, Pacing.FAST, Set.of());
        saveReview(fastWarned.getId(), 1L, Pacing.FAST, Set.of("violence"));
        saveReview(fastWarned.getId(), 2L, Pacing.FAST, Set.of());
        saveReview(slowSafe.getId(), 1L, Pacing.SLOW, Set.of());
        saveReview(slowSafe.getId(), 2L, Pacing.SLOW, Set.of());

        Page<Book> page = bookRepositoryAdapter.filterBooks(
                BookFilterCriteria.builder()
                        .pacing(Set.of(Pacing.FAST))
                        .contentSafe(true)
                        .build(),
                PageRequest.of(0, 10)
        );

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).extracting(Book::getId)
                .containsExactly(fastSafe.getId());
    }

    @Test
    void search_joinsGenres_andCountSearchMatchesPagedResults() {
        saveBook("Alpha Title", "Zed Author", 4.0, Set.of("Fiction"));
        saveBook("Other Book", "Nobody", 3.0, Set.of("Romance"));
        BookEntity byGenre = saveBook("Random Name", "Writer", 4.2, Set.of("Mystery Thriller"));

        List<Book> page0 = bookRepositoryAdapter.search("thriller", 0, 1);
        List<Book> page1 = bookRepositoryAdapter.search("thriller", 1, 1);
        long total = bookRepositoryAdapter.countSearch("thriller");

        assertThat(total).isEqualTo(1);
        assertThat(page0).extracting(Book::getId).containsExactly(byGenre.getId());
        assertThat(page1).isEmpty();

        List<Book> byAuthor = bookRepositoryAdapter.search("zed", 0, 10);
        assertThat(byAuthor).extracting(Book::getTitle).containsExactly("Alpha Title");
        assertThat(bookRepositoryAdapter.countSearch("zed")).isEqualTo(1);
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
