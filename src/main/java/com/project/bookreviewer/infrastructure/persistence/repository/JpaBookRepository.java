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

    @Query("SELECT DISTINCT g FROM BookEntity b JOIN b.genres g ORDER BY g")
    List<String> findAllGenres();

    @Query("SELECT b FROM BookEntity b WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<BookEntity> search(@Param("query") String query, Pageable pageable);

    // Trending: books with most status updates/reviews in last 7 days (simplified for Day1)
    @Query(value = """
        SELECT b.* FROM books b
        LEFT JOIN user_book_status s ON b.id = s.book_id 
            AND s.updated_at > CURRENT_DATE - INTERVAL '7 days'
        GROUP BY b.id
        ORDER BY COUNT(s.id) DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<BookEntity> findTrending(@Param("limit") int limit);

    // Featured: hardcoded or configurable (for Day1, just pick the first book)
    default Optional<BookEntity> findFeatured() {
        return findTopByOrderByIdAsc();
    }
    Optional<BookEntity> findTopByOrderByIdAsc();
}
