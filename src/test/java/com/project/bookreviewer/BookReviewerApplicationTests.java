package com.project.bookreviewer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Smoke test that the full Spring context starts.
 * H2 + JWT come from {@code src/test/resources/application.yaml};
 * Elasticsearch requires a live node (repositories check indices on init).
 */
@SpringBootTest
@Testcontainers
class BookReviewerApplicationTests {

    private static final DockerImageName ES_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(ES_IMAGE)
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void registerElasticsearch(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.host", ELASTICSEARCH::getHost);
        registry.add("elasticsearch.port", () -> ELASTICSEARCH.getMappedPort(9200));
    }

    @Test
    void contextLoads() {
    }
}
