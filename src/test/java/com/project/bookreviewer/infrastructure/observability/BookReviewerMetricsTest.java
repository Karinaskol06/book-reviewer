package com.project.bookreviewer.infrastructure.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BookReviewerMetricsTest {

    private MeterRegistry registry;
    private BookReviewerMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        metrics = new BookReviewerMetrics(registry);
    }

    @Test
    void recordReviewCreated_incrementsCounter() {
        metrics.recordReviewCreated();
        metrics.recordReviewCreated();

        assertThat(registry.get("bookreviewer.reviews.created").counter().count()).isEqualTo(2.0);
    }

    @Test
    void recordSearchFallback_incrementsCounter() {
        metrics.recordSearchFallback();

        assertThat(registry.get("bookreviewer.search.fallback").counter().count()).isEqualTo(1.0);
    }

    @Test
    void searchTimer_recordsLatencyByBackend() {
        Timer.Sample sample = metrics.startSearchTimer();
        metrics.stopSearchTimer(sample, BookReviewerMetrics.STORE_ELASTICSEARCH);

        assertThat(registry.get("bookreviewer.search.latency")
                .tag("store", BookReviewerMetrics.STORE_ELASTICSEARCH)
                .timer()
                .count()).isEqualTo(1L);
    }

    @Test
    void recordEsSyncFailure_isTaggedByEvent() {
        metrics.recordEsSyncFailure(BookReviewerMetrics.EVENT_REVIEW_CREATED);

        assertThat(registry.get("bookreviewer.es.sync.failures")
                .tag("event", BookReviewerMetrics.EVENT_REVIEW_CREATED)
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    void recordAuthFailure_incrementsCounter() {
        metrics.recordAuthFailure();

        assertThat(registry.get("bookreviewer.auth.failures").counter().count()).isEqualTo(1.0);
    }
}
