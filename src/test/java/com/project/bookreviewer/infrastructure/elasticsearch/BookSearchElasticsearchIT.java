package com.project.bookreviewer.infrastructure.elasticsearch;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.service.SearchService;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.infrastructure.elasticsearch.document.BookDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves {@link SearchService#searchBooks} hits real Elasticsearch (not the JPA fallback).
 * Postgres is not required for this path: ES returns hits, then books are loaded via {@link BookRepositoryPort#findById}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class BookSearchElasticsearchIT {

    private static final DockerImageName ES_IMAGE = DockerImageName
            .parse("docker.elastic.co/elasticsearch/elasticsearch:8.11.0");

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(ES_IMAGE)
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("elasticsearch.host", ELASTICSEARCH::getHost);
        registry.add("elasticsearch.port", () -> ELASTICSEARCH.getMappedPort(9200));
        // Boot still needs a datasource + JWT to start the app context.
        registry.add("spring.datasource.url",
                () -> "jdbc:h2:mem:es_search_it;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE");
        registry.add("spring.datasource.username", () -> "sa");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.h2.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.jpa.properties.hibernate.dialect", () -> "org.hibernate.dialect.H2Dialect");
        registry.add("jwt.secret", () -> "0123456789abcdef0123456789abcdef");
        registry.add("jwt.expiration", () -> "3600000");
        registry.add("app.storage.type", () -> "local");
    }

    @Autowired
    private SearchService searchService;
    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @MockBean
    private BookRepositoryPort bookRepository;

    @BeforeEach
    void setUp() {
        IndexOperations indexOps = elasticsearchOperations.indexOps(BookDocument.class);
        if (indexOps.exists()) {
            indexOps.delete();
        }
        indexOps.createWithMapping();
    }

    @Test
    void searchBooks_usesElasticsearchHits_notJpaFallback() {
        BookDocument indexed = BookDocument.builder()
                .id(42L)
                .title("Dune Messiah")
                .author("Frank Herbert")
                .description("Sci-fi sequel")
                .genres(Set.of("Sci Fi"))
                .averageRating(4.6)
                .publicationYear(1969)
                .build();
        elasticsearchOperations.save(indexed);
        elasticsearchOperations.indexOps(BookDocument.class).refresh();

        Book domainBook = Book.builder()
                .id(42L)
                .title("Dune Messiah")
                .author("Frank Herbert")
                .genres(Set.of("Sci Fi"))
                .averageRating(4.6)
                .build();
        when(bookRepository.findById(42L)).thenReturn(Optional.of(domainBook));

        Page<BookResponse> page = null;
        AssertionError lastFailure = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try {
                page = searchService.searchBooks("Dune", PageRequest.of(0, 10));
                assertThat(page.getContent()).extracting(BookResponse::getId).contains(42L);
                assertThat(page.getContent()).extracting(BookResponse::getTitle).contains("Dune Messiah");
                lastFailure = null;
                break;
            } catch (AssertionError ex) {
                lastFailure = ex;
                try {
                    Thread.sleep(250);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(ie);
                }
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }

        verify(bookRepository, never()).search(anyString(), anyInt(), anyInt());
        verify(bookRepository, never()).countSearch(anyString());
    }
}
