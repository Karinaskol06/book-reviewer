package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort.BookRecommendationHit;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserBookStatusRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {
    private static final int MAX_LIMIT = 20;
    private static final int MIN_LIMIT = 1;
    private static final int REVIEW_PAGE_SIZE = 500;

    private final BookSearchPort bookSearchPort;
    private final BookRepositoryPort bookRepository;
    private final UserBookStatusRepositoryPort statusRepository;
    private final ReviewRepositoryPort reviewRepository;
    private final BookMapper bookMapper;
    private final TasteProfileService tasteProfileService;

    @Transactional(readOnly = true)
    public List<BookResponse> getRecommendations(Long userId, int limit) {
        int clamped = clampLimit(limit);
        Set<Long> excluded = buildExcludedBookIds(userId);

        Optional<List<BookRecommendationHit>> fromSearch =
                bookSearchPort.findRecommendations(userId, excluded, clamped);
        if (fromSearch.isPresent() && !fromSearch.get().isEmpty()) {
            List<BookResponse> mapped = mapHits(fromSearch.get(), excluded, clamped);
            if (!mapped.isEmpty()) {
                return mapped;
            }
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

    private List<BookResponse> mapHits(
            List<BookRecommendationHit> hits,
            Set<Long> excluded,
            int limit
    ) {
        List<BookResponse> results = new ArrayList<>();
        for (BookRecommendationHit hit : hits) {
            if (hit == null || hit.bookId() == null || excluded.contains(hit.bookId())) {
                continue;
            }
            Book book = bookRepository.findById(hit.bookId()).orElse(null);
            if (book == null) {
                continue;
            }
            BookResponse resp = bookMapper.toResponse(book);
            if (resp == null) {
                continue;
            }
            if (hit.recommendationReason() != null && !hit.recommendationReason().isBlank()) {
                resp.setRecommendationReason(hit.recommendationReason());
            }
            results.add(resp);
            if (results.size() >= limit) {
                break;
            }
        }
        return results;
    }

    List<BookResponse> fromPostgresFallback(Long userId, int limit, Set<Long> excluded) {
        List<String> tasteGenres = tasteProfileService.resolveTasteGenreLabels(userId, 3);
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

        List<Book> trending = bookRepository.findTrending(limit + excluded.size(), null);
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
}
