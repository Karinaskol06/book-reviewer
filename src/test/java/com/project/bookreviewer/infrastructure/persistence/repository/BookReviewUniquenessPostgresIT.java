package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import com.project.bookreviewer.infrastructure.persistence.support.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Real-Postgres coverage for unique constraints that H2/mocks can under-test.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class BookReviewUniquenessPostgresIT extends AbstractPostgresDataJpaTest {

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
    void books_rejectDuplicateNormalizedTitleAndAuthor() {
        jpaBookRepository.saveAndFlush(book("Dune", "dune", "Herbert", "herbert"));

        assertThatThrownBy(() -> jpaBookRepository.saveAndFlush(
                book("DUNE", "dune", "HERBERT", "herbert")
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void reviews_rejectDuplicateUserAndBook() {
        BookEntity book = jpaBookRepository.saveAndFlush(book("Book", "book", "Author", "author"));
        jpaReviewRepository.saveAndFlush(review(1L, book.getId()));

        assertThatThrownBy(() -> jpaReviewRepository.saveAndFlush(review(1L, book.getId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private static BookEntity book(String title, String normalizedTitle, String author, String normalizedAuthor) {
        return BookEntity.builder()
                .title(title)
                .normalizedTitle(normalizedTitle)
                .author(author)
                .normalizedAuthor(normalizedAuthor)
                .description("desc")
                .publicationYear(2020)
                .genres(Set.of("Fiction"))
                .averageRating(4.0)
                .ratingCount(0)
                .totalReviews(0)
                .build();
    }

    private static ReviewEntity review(Long userId, Long bookId) {
        return ReviewEntity.builder()
                .userId(userId)
                .bookId(bookId)
                .rating(5)
                .verdict("Good")
                .build();
    }
}
