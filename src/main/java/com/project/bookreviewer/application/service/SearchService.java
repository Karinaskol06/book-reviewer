package com.project.bookreviewer.application.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.infrastructure.elasticsearch.document.BookDocument;
import com.project.bookreviewer.infrastructure.elasticsearch.document.ReviewDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    private final ElasticsearchOperations elasticsearchOperations;
    private final BookRepositoryPort bookRepository; // fallback
    private final BookMapper bookMapper;

    public Page<BookResponse> searchBooks(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }
        String normalizedQuery = query.trim();
        String wildcardQuery = "*" + normalizedQuery.toLowerCase() + "*";
        try {
            // Build Elasticsearch query
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
                    .withPageable(pageable)
                    .build();

            SearchHits<BookDocument> searchHits = elasticsearchOperations.search(nativeQuery, BookDocument.class);
            List<BookResponse> books = mapToBookResponses(searchHits);

            return new PageImpl<>(books, pageable, searchHits.getTotalHits());
        } catch (Exception e) {
            log.error("Elasticsearch unavailable, falling back to PostgreSQL search", e);
            return fallbackSearch(query, pageable);
        }
    }

    private Page<BookResponse> fallbackSearch(String query, Pageable pageable) {
        List<Book> books = bookRepository.search(query, pageable.getPageNumber(), pageable.getPageSize());
        return new PageImpl<>(
                books.stream().map(bookMapper::toResponse).collect(Collectors.toList()),
                pageable,
                books.size()
        );
    }

    public Page<BookResponse> filterBooks(BookFilterCriteria criteria, Pageable pageable) {
        // If no filters provided, just return all books
        if (isEmptyCriteria(criteria)) {
            List<Book> books = bookRepository.findAll(pageable.getPageNumber(), pageable.getPageSize());
            long total = bookRepository.count();
            Page<Book> page = new PageImpl<>(books, pageable, total);
            return page.map(bookMapper::toResponse);
        }

        // Authoritative cached average lives in PostgreSQL; ES can lag after reviews, so minRating must use JPA.
        if (criteria.getMinRating() != null && criteria.getMinRating() > 0) {
            Page<Book> books = bookRepository.filterBooks(criteria, pageable);
            return books.map(bookMapper::toResponse);
        }

        try {
            BoolQuery.Builder boolBuilder = new BoolQuery.Builder();

            // Genre filter (converts the set to a list of FieldValue objects required by Elasticsearch)
            if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
                boolBuilder.filter(f -> f.terms(t -> t
                        .field("genres")
                        .terms(terms -> terms.value(
                                criteria.getGenres().stream()
                                        .map(FieldValue::of)
                                        .collect(Collectors.toList())
                        ))
                ));
            }

            // Year range
            if (criteria.getYearFrom() != null) {
                boolBuilder.filter(f -> f.range(r -> r
                        //value of a field in an indexed document used for this range check
                        .field("publicationYear")
                        //greater or equal
                        .gte(JsonData.of(criteria.getYearFrom()))
                ));
            }
            if (criteria.getYearTo() != null) {
                boolBuilder.filter(f -> f.range(r -> r
                        .field("publicationYear")
                        .lte(JsonData.of(criteria.getYearTo()))
                ));
            }

            // Pacing filter
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

            // Content safety
            if (Boolean.TRUE.equals(criteria.getContentSafe())) {
                boolBuilder.filter(f -> f.term(t -> t
                        //loop through this "column"
                        .field("hasContentWarnings")
                        .value(false)
                ));
            }

            // Full-text search within filtered results
            if (criteria.getSearchQuery() != null && !criteria.getSearchQuery().isBlank()) {
                String normalizedSearch = criteria.getSearchQuery().trim();
                String wildcardSearch = "*" + normalizedSearch.toLowerCase() + "*";
                //full-text search
                boolBuilder.must(m -> m.multiMatch(mm -> mm
                        .fields("title^2", "author^1.5", "description")
                        .query(normalizedSearch)
                ));
                //wildcards queries for fuzzy searches
                boolBuilder.should(s -> s.wildcard(w -> w.field("title")
                        .value(wildcardSearch).caseInsensitive(true)));
                boolBuilder.should(s -> s.wildcard(w -> w.field("author")
                        .value(wildcardSearch).caseInsensitive(true)));
                boolBuilder.should(s -> s.wildcard(w -> w.field("description")
                        .value(wildcardSearch).caseInsensitive(true)));
                boolBuilder.should(s -> s.wildcard(w -> w.field("genres")
                        .value(wildcardSearch).caseInsensitive(true)));
            }

            //wrapper (with pagination info)
            NativeQuery nativeQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(boolBuilder.build()))
                    .withPageable(pageable)
                    .build();

            SearchHits<BookDocument> hits = elasticsearchOperations.search(nativeQuery, BookDocument.class);
            List<BookResponse> books = mapToBookResponses(hits);
            return new PageImpl<>(books, pageable, hits.getTotalHits());
        } catch (Exception e) {
            //fallback to postgres
            log.error("ES filter failed, falling back to DB filter", e);
            Page<Book> books = bookRepository.filterBooks(criteria, pageable);
            return books.map(bookMapper::toResponse);
        }
    }

    public List<BookResponse> getRecommendations(Long userId, int limit) {
        try {
            // Find user's highly rated books from ES reviews index
            NativeQuery userRatingsQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(b -> b
                            .must(m -> m.term(t -> t.field("userId").value(userId)))
                            .must(m -> m.range(r -> r.field("rating").gte(JsonData.of(4))))
                    ))
                    .withMaxResults(10)
                    .build();
            SearchHits<ReviewDocument> userReviews = elasticsearchOperations.search(userRatingsQuery, ReviewDocument.class);

            if (userReviews.isEmpty()) return List.of();

            // Collect book IDs the user already rated highly
            Set<Long> likedBookIds = userReviews.stream()
                    .map(hit -> hit.getContent().getBookId())
                    .collect(Collectors.toSet());

            // Fetch the actual BookDocuments for those liked books by book ids
            List<BookDocument> likedBooks = new ArrayList<>();
            for (Long bookId : likedBookIds) {
                BookDocument doc = elasticsearchOperations.get(bookId.toString(), BookDocument.class);
                if (doc != null) likedBooks.add(doc);
            }

            // Criteria for finding similar books
            // Set of unique genre tags from liked books
            Set<String> likedGenres = likedBooks.stream()
                    .flatMap(book -> book.getGenres().stream())
                    .collect(Collectors.toSet());
            // Set of unique pacing values
            Set<String> likedPacing = likedBooks.stream()
                    .map(BookDocument::getDominantPacing)
                    .filter(p -> p != null)
                    .collect(Collectors.toSet());

            // Build recommendation query: books with similar genres/pacing, excluding already read
            BoolQuery.Builder recBool = new BoolQuery.Builder();
            //should = OR; book should match at least one of the liked genres or pacing types
            if (likedGenres.isEmpty() && likedPacing.isEmpty()) {
                return List.of();
            }

            if (!likedGenres.isEmpty()) {
                recBool.should(s -> s.terms(t -> t
                        .field("genres")
                        .terms(terms -> terms.value(
                                likedGenres.stream().map(FieldValue::of).collect(Collectors.toList())
                        ))
                ));
            }
            if (!likedPacing.isEmpty()) {
                recBool.should(s -> s.terms(t -> t
                        .field("dominantPacing")
                        .terms(terms -> terms.value(
                                likedPacing.stream().map(FieldValue::of).collect(Collectors.toList())
                        ))
                ));
            }
            //excludes the books already in liked set
            recBool.mustNot(m -> m.terms(t -> t
                    .field("id")
                    .terms(terms -> terms.value(
                            likedBookIds.stream().map(id -> FieldValue.of(id)).collect(Collectors.toList())
                    ))
            ));

            // Wraps recommendations query into native
            NativeQuery recQuery = NativeQuery.builder()
                    .withQuery(q -> q.bool(recBool.build()))
                    .withMaxResults(limit)
                    .build();

            // Query execution
            SearchHits<BookDocument> recHits = elasticsearchOperations.search(recQuery, BookDocument.class);

            // Mapping search hits to book responses with explanation
            return recHits.stream()
                    .map(SearchHit::getContent)
                    .map(doc -> {
                        //for each hit info from postgres is queried
                        BookResponse resp = bookMapper.toResponse(
                                bookRepository.findById(doc.getId()).orElse(null));
                        if (resp != null) {
                            // Build explanation
                            List<String> reasons = new ArrayList<>();
                            if (likedGenres.stream().anyMatch(g -> doc.getGenres().contains(g))) {
                                reasons.add("Similar genres");
                            }
                            if (doc.getDominantPacing() != null && likedPacing.contains(doc.getDominantPacing())) {
                                reasons.add("Similar pacing");
                            }
                            resp.setRecommendationReason(String.join(" and ", reasons));
                        }
                        return resp;
                    })
                    .filter(resp -> resp != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Recommendations failed", e);
            return List.of(); // or fallback to DB
        }
    }

    public List<String> getAllGenres() {
        return bookRepository.findAllGenres();
    }

    /* helper methods */
    private List<BookResponse> mapToBookResponses(SearchHits<BookDocument> hits) {
        return hits.stream()
                .map(SearchHit::getContent)
                .map(doc -> {
                    Book book = bookRepository.findById(doc.getId()).orElse(null);
                    if (book == null) return null;
                    return bookMapper.toResponse(book);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isEmptyCriteria(BookFilterCriteria criteria) {
        return (criteria.getGenres() == null || criteria.getGenres().isEmpty())
                && criteria.getMinRating() == null
                && (criteria.getPacing() == null || criteria.getPacing().isEmpty())
                && criteria.getYearFrom() == null
                && criteria.getYearTo() == null
                && criteria.getContentSafe() == null
                && (criteria.getSearchQuery() == null || criteria.getSearchQuery().isBlank());
    }
}