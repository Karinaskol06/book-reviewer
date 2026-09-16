package com.project.bookreviewer.application.service;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.json.JsonData;
import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import com.project.bookreviewer.infrastructure.elasticsearch.document.BookDocument;
import com.project.bookreviewer.infrastructure.elasticsearch.document.ReviewDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    private static final int MAX_LIMIT = 20;
    private static final int MIN_LIMIT = 1;
    private static final int HIGH_RATING_THRESHOLD = 4;
    private static final int REVIEW_PAGE_SIZE = 500;

    private final ElasticsearchOperations elasticsearchOperations;
    private final BookRepositoryPort bookRepository;
    private final UserBookStatusRepositoryPort statusRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final BookMapper bookMapper;

    @Transactional(readOnly = true)
    public List<BookResponse> getRecommendations(Long userId, int limit) {
        int clamped = clampLimit(limit);
        Set<Long> excluded = buildExcludedBookIds(userId);

        List<BookResponse> fromEs = fromElasticsearch(userId, clamped, excluded);
        if (!fromEs.isEmpty()) {
            return fromEs;
        }
        return fromPostgresFallback(userId, clamped, excluded);
    }

    int clampLimit(int limit) {
        if (limit < MIN_LIMIT) {
            return MIN_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    Set<Long> buildExcludedBookIds(Long userId) {
        Set<Long> excluded = new HashSet<>();
        statusRepository.findByUserId(userId).stream()
                .map(UserBookStatus::getBookId)
                .filter(Objects::nonNull)
                .forEach(excluded::add);
        reviewRepository.findByUserId(userId, PageRequest.of(0, REVIEW_PAGE_SIZE)).getContent().stream()
                .map(Review::getBookId)
                .filter(Objects::nonNull)
                .forEach(excluded::add);
        return excluded;
    }

    private List<BookResponse> fromElasticsearch(Long userId, int limit, Set<Long> excluded) {
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
                return List.of();
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
                return List.of();
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

            Set<Long> mustNotIds = new HashSet<>(excluded);
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

            List<BookResponse> results = new ArrayList<>();
            for (SearchHit<BookDocument> hit : recHits) {
                BookDocument doc = hit.getContent();
                if (doc == null || doc.getId() == null || excluded.contains(doc.getId())) {
                    continue;
                }
                Book book = bookRepository.findById(doc.getId()).orElse(null);
                if (book == null) {
                    continue;
                }
                BookResponse resp = bookMapper.toResponse(book);
                if (resp == null) {
                    continue;
                }
                List<String> reasons = new ArrayList<>();
                if (doc.getGenres() != null && likedGenres.stream().anyMatch(g -> doc.getGenres().contains(g))) {
                    reasons.add("Similar genres");
                }
                if (doc.getDominantPacing() != null && likedPacing.contains(doc.getDominantPacing())) {
                    reasons.add("Similar pacing");
                }
                if (!reasons.isEmpty()) {
                    resp.setRecommendationReason(String.join(" and ", reasons));
                }
                results.add(resp);
                if (results.size() >= limit) {
                    break;
                }
            }
            return results;
        } catch (Exception e) {
            log.error("Elasticsearch recommendations failed for userId={}", userId, e);
            return List.of();
        }
    }

    List<BookResponse> fromPostgresFallback(Long userId, int limit, Set<Long> excluded) {
        List<String> tasteGenres = resolveTasteGenres(userId);
        LinkedHashMap<Long, BookResponse> assembled = new LinkedHashMap<>();

        for (String genre : tasteGenres) {
            List<Book> candidates = bookRepository.findByGenre(genre, 0, limit + excluded.size());
            for (Book book : candidates) {
                if (book == null || book.getId() == null || excluded.contains(book.getId())) {
                    continue;
                }
                if (assembled.containsKey(book.getId())) {
                    continue;
                }
                BookResponse response = bookMapper.toResponse(book);
                if (response == null) {
                    continue;
                }
                response.setRecommendationReason("Because you enjoy " + genre);
                assembled.put(book.getId(), response);
                if (assembled.size() >= limit) {
                    return new ArrayList<>(assembled.values());
                }
            }
        }

        List<Book> trending = bookRepository.findTrending(limit + excluded.size());
        for (Book book : trending) {
            if (book == null || book.getId() == null || excluded.contains(book.getId())) {
                continue;
            }
            if (assembled.containsKey(book.getId())) {
                continue;
            }
            BookResponse response = bookMapper.toResponse(book);
            if (response == null) {
                continue;
            }
            response.setRecommendationReason("Trending in the archive");
            assembled.put(book.getId(), response);
            if (assembled.size() >= limit) {
                break;
            }
        }

        return new ArrayList<>(assembled.values());
    }

    private List<String> resolveTasteGenres(Long userId) {
        Map<String, Integer> shelfGenreCounts = new HashMap<>();
        for (UserBookStatus status : statusRepository.findByUserId(userId)) {
            bookRepository.findById(status.getBookId()).ifPresent(book -> countGenres(book, shelfGenreCounts));
        }
        if (!shelfGenreCounts.isEmpty()) {
            return topGenres(shelfGenreCounts, 3);
        }

        Map<String, Integer> reviewGenreCounts = new HashMap<>();
        List<Review> reviews = reviewRepository.findByUserId(userId, PageRequest.of(0, REVIEW_PAGE_SIZE)).getContent();
        for (Review review : reviews) {
            if (review.getRating() == null || review.getRating() < HIGH_RATING_THRESHOLD) {
                continue;
            }
            bookRepository.findById(review.getBookId()).ifPresent(book -> countGenres(book, reviewGenreCounts));
        }
        if (!reviewGenreCounts.isEmpty()) {
            return topGenres(reviewGenreCounts, 3);
        }
        return List.of();
    }

    private void countGenres(Book book, Map<String, Integer> counts) {
        if (book.getGenres() == null) {
            return;
        }
        for (String genre : book.getGenres()) {
            if (genre == null || genre.isBlank()) {
                continue;
            }
            counts.merge(genre, 1, Integer::sum);
        }
    }

    private List<String> topGenres(Map<String, Integer> counts, int limit) {
        return counts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(Map.Entry::getKey))
                .limit(limit)
                .map(Map.Entry::getKey)
                .toList();
    }
}
