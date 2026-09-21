package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.BookEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface JpaBookRepository extends JpaRepository<BookEntity, Long> {
    Page<BookEntity> findAll(Specification<BookEntity> spec, Pageable pageable);

    Optional<BookEntity> findByNormalizedTitleAndNormalizedAuthor(String normalizedTitle, String normalizedAuthor);

    boolean existsByNormalizedTitleAndNormalizedAuthor(String normalizedTitle, String normalizedAuthor);

    @Query("SELECT b FROM BookEntity b JOIN b.genres g WHERE g = :genre")
    List<BookEntity> findByGenre(@Param("genre") String genre, Pageable pageable);

    @Query("SELECT DISTINCT g FROM BookEntity b JOIN b.genres g")
    List<String> findAllGenres();

    @Query("SELECT DISTINCT b FROM BookEntity b LEFT JOIN b.genres g " +
            "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(g) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<BookEntity> search(@Param("query") String query, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT b) FROM BookEntity b LEFT JOIN b.genres g " +
            "WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(b.description) LIKE LOWER(CONCAT('%', :query, '%')) " +
            "OR LOWER(g) LIKE LOWER(CONCAT('%', :query, '%'))")
    long countSearch(@Param("query") String query);

    /**
     * Home page trending list - most popular books first
     *
     * Builds counts per book from reviews and recent shelf activity, then sorts:
     * how many reviews in the last 7 days
     * how many reviews in the last 30 days
     * cached average rating on the book
     * all-time review count
     * shelf status updates in the last 7 days
     * most recent review time, then newest book as final ties
     *
     * If excludeUserId is set, books already on that user's shelf are left out.
     * Only the top limit rows are returned.
     */
    @Query(value = """
        SELECT b.*
        FROM books b
        LEFT JOIN (
            SELECT r.book_id,
                   COUNT(*) AS review_count_7d
            FROM reviews r
            WHERE r.created_at > CURRENT_TIMESTAMP - INTERVAL '7' DAY
            GROUP BY r.book_id
        ) reviews_7d ON reviews_7d.book_id = b.id
        LEFT JOIN (
            SELECT r.book_id,
                   COUNT(*) AS review_count_30d
            FROM reviews r
            WHERE r.created_at > CURRENT_TIMESTAMP - INTERVAL '30' DAY
            GROUP BY r.book_id
        ) reviews_30d ON reviews_30d.book_id = b.id
        LEFT JOIN (
            SELECT r.book_id,
                   COUNT(*) AS review_count,
                   MAX(r.created_at) AS latest_review_at
            FROM reviews r
            GROUP BY r.book_id
        ) review_stats ON review_stats.book_id = b.id
        LEFT JOIN (
            SELECT s.book_id,
                   COUNT(*) AS status_activity_count
            FROM user_book_status s
            WHERE s.updated_at > CURRENT_TIMESTAMP - INTERVAL '7' DAY
            GROUP BY s.book_id
        ) status_stats ON status_stats.book_id = b.id
        WHERE (:excludeUserId IS NULL OR b.id NOT IN (
            SELECT s.book_id FROM user_book_status s WHERE s.user_id = :excludeUserId
        ))
        ORDER BY COALESCE(reviews_7d.review_count_7d, 0) DESC,
                 COALESCE(reviews_30d.review_count_30d, 0) DESC,
                 COALESCE(b.average_rating, 0) DESC,
                 COALESCE(review_stats.review_count, 0) DESC,
                 COALESCE(status_stats.status_activity_count, 0) DESC,
                 review_stats.latest_review_at DESC NULLS LAST,
                 b.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<BookEntity> findTrending(@Param("limit") int limit, @Param("excludeUserId") Long excludeUserId);
}
