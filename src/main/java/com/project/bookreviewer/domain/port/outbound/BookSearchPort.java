package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.BookFilterCriteria;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Outbound port for full-text / filtered book search and similarity recommendations.
 * Implementations may use Elasticsearch; {@link Optional#empty()} means the search
 * backend is unavailable and the application should fall back to Postgres.
 */
public interface BookSearchPort {

    Optional<BookSearchPage> searchBooks(String query, int page, int size);

    Optional<BookSearchPage> filterBooks(
            BookFilterCriteria criteria,
            Set<String> expandedGenres,
            int page,
            int size
    );

    /**
     * @return ranked recommendation hits with reasons, or empty if unavailable / no signal
     */
    Optional<List<BookRecommendationHit>> findRecommendations(
            Long userId,
            Set<Long> excludedBookIds,
            int limit
    );

    record BookSearchPage(List<Long> bookIds, long totalHits) {
    }

    record BookRecommendationHit(Long bookId, String recommendationReason) {
    }
}
