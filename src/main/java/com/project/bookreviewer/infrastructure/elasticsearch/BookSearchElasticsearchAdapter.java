package com.project.bookreviewer.infrastructure.elasticsearch;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort;
import com.project.bookreviewer.infrastructure.elasticsearch.document.BookDocument;
import com.project.bookreviewer.infrastructure.elasticsearch.document.ReviewDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookSearchElasticsearchAdapter implements BookSearchPort {

    private static final int HIGH_RATING_THRESHOLD = 4;

    private final ElasticsearchOperations elasticsearchOperations;

    @Override
    public Optional<BookSearchPage> searchBooks(String query, int page, int size) {
        if (query == null || query.isBlank()) {
            return Optional.of(new BookSearchPage(List.of(), 0L));
        }
        String normalizedQuery = query.trim();
        String wildcardQuery = "*" + normalizedQuery.toLowerCase() + "*";
        try {
            var boolQuery = BoolQuery.of(b -> b
                    .should(s -> s.match(m -> m.field("title").query(normalizedQuery).boost(2.0f)))
                    .should(s -> s.match(m -> m.field("author").query(normalizedQuery).boost(1.5f)))
                    .should(s -> s.match(m -> m.field("description").query(normalizedQuery)))
                    .should(s -> s.wildcard(w -> w.field("title").value(wildcardQuery).caseInsensitive(true)))
                    .should(s -> s.wildcard(w -> w.field("author").value(wildcardQuery).caseInsensitive(true)))
                    .should(s -> s.wildcard(w -> w.field("description").value(wildcardQuery).caseInsensitive(true)))
                    .should(s -> s.wildcard(w -> w.field("genres").value(wildcardQuery).caseInsensitive(true)))
                    .minimumShouldMatch("1")
            );

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolQuery))
                    .withPageable(PageRequest.of(page, size))
                    .build();

            SearchHits<BookDocument> searchHits =
                    elasticsearchOperations.search(nativeQuery, BookDocument.class);
            return Optional.of(toSearchPage(searchHits));
        } catch (Exception e) {
            log.error("Elasticsearch search failed", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<BookSearchPage> filterBooks(
            BookFilterCriteria criteria,
            Set<String> expandedGenres,
            int page,
            int size
    ) {
        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
                if (expandedGenres == null || expandedGenres.isEmpty()) {
                    return Optional.of(new BookSearchPage(List.of(), 0L));
                }
                boolBuilder.filter(f -> f.terms(t -> t
                        .field("genres")
                        .terms(terms -> terms.value(
                                expandedGenres.stream()
                                        .map(FieldValue::of)
                                        .collect(Collectors.toList())
                        ))
                ));
            }

            if (criteria.getMinRating() != null && criteria.getMinRating() > 0) {
                boolBuilder.filter(f -> f.range(r -> r
                        .field("averageRating")
                        .gte(JsonData.of(criteria.getMinRating().doubleValue()))
                ));
            }

            if (criteria.getYearFrom() != null) {
                boolBuilder.filter(f -> f.range(r -> r
                        .field("publicationYear")
                        .gte(JsonData.of(criteria.getYearFrom()))
                ));
            }
            if (criteria.getYearTo() != null) {
                boolBuilder.filter(f -> f.range(r -> r
                        .field("publicationYear")
                        .lte(JsonData.of(criteria.getYearTo()))
                ));
            }

            if (criteria.getPacing() != null && !criteria.getPacing().isEmpty()) {
                Set<String> pacingStrings = criteria.getPacing().stream()
                        .map(Enum::name)
                        .collect(Collectors.toSet());
                boolBuilder.filter(f -> f.terms(t -> t
                        .field("dominantPacing")
                        .terms(terms -> terms.value(
                                pacingStrings.stream()
                                        .map(FieldValue::of)
                                        .collect(Collectors.toList())
                        ))
                ));
            }

            if (Boolean.TRUE.equals(criteria.getContentSafe())) {
                boolBuilder.filter(f -> f.term(t -> t
                        .field("hasContentWarnings")
                        .value(false)
                ));
            }

            if (criteria.getSearchQuery() != null && !criteria.getSearchQuery().isBlank()) {
                String normalizedSearch = criteria.getSearchQuery().trim();
                String wildcardSearch = "*" + normalizedSearch.toLowerCase() + "*";
                boolBuilder.must(m -> m.multiMatch(mm -> mm
                        .fields("title^2", "author^1.5", "description")
                        .query(normalizedSearch)
                ));
                boolBuilder.should(s -> s.wildcard(w -> w.field("title")
                        .value(wildcardSearch).caseInsensitive(true)));
                boolBuilder.should(s -> s.wildcard(w -> w.field("author")
                        .value(wildcardSearch).caseInsensitive(true)));
                boolBuilder.should(s -> s.wildcard(w -> w.field("description")
                        .value(wildcardSearch).caseInsensitive(true)));
                boolBuilder.should(s -> s.wildcard(w -> w.field("genres")
                        .value(wildcardSearch).caseInsensitive(true)));
            }

            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolBuilder.build()))
                    .withPageable(PageRequest.of(page, size))
                    .build();

            SearchHits<BookDocument> hits =
                    elasticsearchOperations.search(nativeQuery, BookDocument.class);
            return Optional.of(toSearchPage(hits));
        } catch (Exception e) {
            log.error("Elasticsearch filter failed", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<List<BookRecommendationHit>> findRecommendations(
            Long userId,
            Set<Long> excludedBookIds,
            int limit
    ) {
        try {
            NativeQuery userRatingsQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("userId").value(userId)))
                            .must(m -> m.range(r -> r.field("rating").gte(JsonData.of(HIGH_RATING_THRESHOLD))))
                    ))
                    .withMaxResults(10)
                    .build();
            SearchHits<ReviewDocument> userReviews =
                    elasticsearchOperations.search(userRatingsQuery, ReviewDocument.class);

            if (userReviews.isEmpty()) {
                return Optional.of(List.of());
            }

            Set<Long> likedBookIds = userReviews.stream()
                    .map(hit -> hit.getContent().getBookId())
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<BookDocument> likedBooks = new ArrayList<>();
            for (Long bookId : likedBookIds) {
                BookDocument doc = elasticsearchOperations.get(bookId.toString(), BookDocument.class);
                if (doc != null) {
                    likedBooks.add(doc);
                }
            }

            Set<String> likedGenres = likedBooks.stream()
                    .filter(book -> book.getGenres() != null)
                    .flatMap(book -> book.getGenres().stream())
                    .collect(Collectors.toSet());
            Set<String> likedPacing = likedBooks.stream()
                    .map(BookDocument::getDominantPacing)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            if (likedGenres.isEmpty() && likedPacing.isEmpty()) {
                return Optional.of(List.of());
            }

            BoolQuery.Builder recBool = new BoolQuery.Builder();
            if (!likedGenres.isEmpty()) {
                recBool.should(s -> s.terms(t -> t
                        .field("genres")
                        .terms(terms -> terms.value(
                                likedGenres.stream().map(FieldValue::of).toList()
                        ))
                ));
            }
            if (!likedPacing.isEmpty()) {
                recBool.should(s -> s.terms(t -> t
                        .field("dominantPacing")
                        .terms(terms -> terms.value(
                                likedPacing.stream().map(FieldValue::of).toList()
                        ))
                ));
            }

            Set<Long> mustNotIds = new HashSet<>(excludedBookIds == null ? Set.of() : excludedBookIds);
            mustNotIds.addAll(likedBookIds);
            if (!mustNotIds.isEmpty()) {
                recBool.mustNot(m -> m.terms(t -> t
                        .field("id")
                        .terms(terms -> terms.value(
                                mustNotIds.stream().map(FieldValue::of).toList()
                        ))
                ));
            }

            NativeQuery recQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(recBool.build()))
                    .withMaxResults(limit)
                    .build();

            SearchHits<BookDocument> recHits =
                    elasticsearchOperations.search(recQuery, BookDocument.class);

            List<BookRecommendationHit> results = new ArrayList<>();
            for (SearchHit<BookDocument> hit : recHits) {
                BookDocument doc = hit.getContent();
                if (doc == null || doc.getId() == null) {
                    continue;
                }
                if (excludedBookIds != null && excludedBookIds.contains(doc.getId())) {
                    continue;
                }

                List<String> reasons = new ArrayList<>();
                if (doc.getGenres() != null && likedGenres.stream().anyMatch(g -> doc.getGenres().contains(g))) {
                    reasons.add("Similar genres");
                }
                if (doc.getDominantPacing() != null && likedPacing.contains(doc.getDominantPacing())) {
                    reasons.add("Similar pacing");
                }
                String reason = reasons.isEmpty() ? null : String.join(" and ", reasons);
                results.add(new BookRecommendationHit(doc.getId(), reason));
                if (results.size() >= limit) {
                    break;
                }
            }
            return Optional.of(results);
        } catch (Exception e) {
            log.error("Elasticsearch recommendations failed for userId={}", userId, e);
            return Optional.empty();
        }
    }

    private static BookSearchPage toSearchPage(SearchHits<BookDocument> hits) {
        List<Long> ids = hits.stream()
                .map(SearchHit::getContent)
                .filter(Objects::nonNull)
                .map(BookDocument::getId)
                .filter(Objects::nonNull)
                .toList();
        return new BookSearchPage(ids, hits.getTotalHits());
    }
}
