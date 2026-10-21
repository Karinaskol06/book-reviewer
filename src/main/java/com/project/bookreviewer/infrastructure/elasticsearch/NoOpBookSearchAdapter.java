package com.project.bookreviewer.infrastructure.elasticsearch;

import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Used when Elasticsearch is disabled (small EC2 instance)
 * Optional#empty() triggers PostgreSQL fallback in {@code SearchService}
 */
@Component
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "false")
public class NoOpBookSearchAdapter implements BookSearchPort {

    @Override
    public Optional<BookSearchPage> searchBooks(String query, int page, int size) {
        return Optional.empty();
    }

    @Override
    public Optional<BookSearchPage> filterBooks(
            BookFilterCriteria criteria,
            Set<String> expandedGenres,
            int page,
            int size
    ) {
        return Optional.empty();
    }

    @Override
    public Optional<List<BookRecommendationHit>> findRecommendations(
            Long userId,
            Set<Long> excludedBookIds,
            int limit
    ) {
        return Optional.empty();
    }
}
