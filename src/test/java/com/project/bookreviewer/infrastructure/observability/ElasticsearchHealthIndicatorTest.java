package com.project.bookreviewer.infrastructure.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.cluster.ClusterHealth;
import org.springframework.data.elasticsearch.core.cluster.ClusterOperations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ElasticsearchHealthIndicatorTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @Mock
    private ClusterOperations clusterOperations;

    @Mock
    private ClusterHealth clusterHealth;

    @InjectMocks
    private ElasticsearchHealthIndicator indicator;

    @Test
    void health_whenClusterReachable_returnsUp() {
        when(elasticsearchOperations.cluster()).thenReturn(clusterOperations);
        when(clusterOperations.health()).thenReturn(clusterHealth);
        when(clusterHealth.getClusterName()).thenReturn("docker-cluster");
        when(clusterHealth.getStatus()).thenReturn(null);
        when(clusterHealth.getNumberOfNodes()).thenReturn(1);

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("clusterName", "docker-cluster");
        assertThat(health.getDetails()).containsEntry("numberOfNodes", 1);
        assertThat(health.getDetails()).containsEntry("status", "null");
    }

    @Test
    void health_whenClusterUnreachable_returnsDegraded() {
        when(elasticsearchOperations.cluster()).thenReturn(clusterOperations);
        when(clusterOperations.health()).thenThrow(new IllegalStateException("connection refused"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(ElasticsearchHealthIndicator.DEGRADED);
        assertThat(health.getDetails()).containsEntry(
                "reason",
                "Elasticsearch unavailable; search falls back to PostgreSQL"
        );
    }
}
