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

    Optional<BookEntity> findByNormalizedTitleAndAuthor(String normalizedTitle, String author);

    boolean existsByNormalizedTitleAndAuthor(String normalizedTitle, String author);

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

    // Trending: primarily by review volume, with recent status activity as secondary signal.
    @Query(value = """
        SELECT b.*
        FROM books b
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
            WHERE s.updated_at > CURRENT_DATE - INTERVAL '7 days'
            GROUP BY s.book_id
        ) status_stats ON status_stats.book_id = b.id
        ORDER BY COALESCE(review_stats.review_count, 0) DESC,
                 COALESCE(status_stats.status_activity_count, 0) DESC,
                 review_stats.latest_review_at DESC NULLS LAST,
                 b.created_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<BookEntity> findTrending(@Param("limit") int limit);

    // Featured: hardcoded or configurable (for Day1, just pick the first book)
    default Optional<BookEntity> findFeatured() {
        return findTopByOrderByIdAsc();
    }
    Optional<BookEntity> findTopByOrderByIdAsc();
}
