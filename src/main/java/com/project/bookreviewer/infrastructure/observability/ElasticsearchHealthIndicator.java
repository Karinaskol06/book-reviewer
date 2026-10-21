package com.project.bookreviewer.infrastructure.observability;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.stereotype.Component;

/**
 * Elasticsearch is optional for serving traffic (search falls back to PostgreSQL).
 * Report {@code DEGRADED} when the cluster is unreachable instead of {@code DOWN},
 * so orchestrators and overall health stay green while still flagging search risk.
 */
@Component("elasticsearch")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchHealthIndicator implements HealthIndicator {

    static final Status DEGRADED = new Status("DEGRADED");

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Health health() {
        try {
            ClusterHealth clusterHealth = elasticsearchOperations.cluster().health();
            return Health.up()
                    .withDetail("clusterName", clusterHealth.getClusterName())
                    .withDetail("status", String.valueOf(clusterHealth.getStatus()))
                    .withDetail("numberOfNodes", clusterHealth.getNumberOfNodes())
                    .build();
        } catch (Exception ex) {
            return Health.status(DEGRADED)
                    .withDetail("reason", "Elasticsearch unavailable; search falls back to PostgreSQL")
                    .withDetail("error", ex.getClass().getSimpleName() + ": " + ex.getMessage())
                    .build();
        }
    }
}
