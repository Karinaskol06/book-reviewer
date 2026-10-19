package com.project.bookreviewer.infrastructure.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * Domain-specific Micrometer meters for Book Reviewer.
 * Tags stay low-cardinality (no userId / free-text query).
 */
@Component
public class BookReviewerMetrics {

    // Fixed tags for the metrics
    public static final String STORE_ELASTICSEARCH = "elasticsearch";
    public static final String STORE_POSTGRESQL = "postgresql";

    public static final String EVENT_BOOK_CREATED = "book_created";
    public static final String EVENT_BOOK_UPDATED = "book_updated";
    public static final String EVENT_REVIEW_CREATED = "review_created";

    private final MeterRegistry registry;
    private final Counter reviewsCreated;
    private final Counter searchFallback;
    private final Counter authFailures;

    public BookReviewerMetrics(MeterRegistry registry) {
        this.registry = registry;
        this.reviewsCreated = Counter.builder("bookreviewer.reviews.created")
                .description("Number of reviews successfully created")
                .register(registry);
        this.searchFallback = Counter.builder("bookreviewer.search.fallback")
                .description("Search requests that fell back from Elasticsearch to PostgreSQL")
                .tag("store", STORE_POSTGRESQL)
                .register(registry);
        this.authFailures = Counter.builder("bookreviewer.auth.failures")
                .description("Failed authentication attempts")
                .tag("reason", "invalid_credentials")
                .register(registry);
    }

    public void recordReviewCreated() {
        reviewsCreated.increment();
    }

    public void recordSearchFallback() {
        searchFallback.increment();
    }

    public Timer.Sample startSearchTimer() {
        return Timer.start(registry);
    }

    public void recordAuthFailure() {
        authFailures.increment();
    }

    public void stopSearchTimer(Timer.Sample sample, String store) {
        // One in-progress timer per search request
        sample.stop(Timer.builder("bookreviewer.search.latency")
                .description("End-to-end book search latency by store")
                .tag("store", store)
                .publishPercentileHistogram()
                .register(registry));
    }

    public void recordEsSyncFailure(String event) {
        Counter.builder("bookreviewer.es.sync.failures")
                .description("Failed Elasticsearch index sync attempts after domain events")
                .tag("event", event)
                .register(registry)
                .increment();
    }
}
