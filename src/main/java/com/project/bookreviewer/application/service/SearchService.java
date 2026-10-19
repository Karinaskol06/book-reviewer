package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.port.inbound.SearchUseCase;
import com.project.bookreviewer.domain.port.outbound.BookRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort;
import com.project.bookreviewer.domain.port.outbound.BookSearchPort.BookSearchPage;
import com.project.bookreviewer.infrastructure.observability.BookReviewerMetrics;
import com.project.bookreviewer.shared.util.NormalizationUtils;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService implements SearchUseCase {
    private final BookSearchPort bookSearchPort;
    private final BookRepositoryPort bookRepository;
    private final BookMapper bookMapper;
    private final BookReviewerMetrics metrics;

    public Page<BookResponse> searchBooks(String query, Pageable pageable) {
        if (query == null || query.isBlank()) {
            return Page.empty(pageable);
        }

        // Notes the start time, returns a sample object that can be used to stop the timer
        Timer.Sample sample = metrics.startSearchTimer();
        String store = BookReviewerMetrics.STORE_ELASTICSEARCH;
        try {
            Optional<BookSearchPage> esPage = bookSearchPort.searchBooks(
                    query,
                    pageable.getPageNumber(),
                    pageable.getPageSize()
            );
            if (esPage.isEmpty()) {
                log.warn("Search backend unavailable, falling back to PostgreSQL search");
                // Records the fallback to PostgreSQL search
                metrics.recordSearchFallback();
                // Sets the store to PostgreSQL
                store = BookReviewerMetrics.STORE_POSTGRESQL;
                // Performs the fallback search
                return fallbackSearch(query, pageable);
            }
            // Converts the Elasticsearch page to a BookResponse page
            return toBookResponsePage(esPage.get(), pageable);
            // Stops the timer and records the latency
        } finally {
            metrics.stopSearchTimer(sample, store);
        }
    }

    private Page<BookResponse> fallbackSearch(String query, Pageable pageable) {
        List<Book> books = bookRepository.search(query, pageable.getPageNumber(), pageable.getPageSize());
        long total = bookRepository.countSearch(query);
        return new PageImpl<>(
                books.stream().map(bookMapper::toResponse).collect(Collectors.toList()),
                pageable,
                total
        );
    }

    public Page<BookResponse> filterBooks(BookFilterCriteria criteria, Pageable pageable) {
        if (isEmptyCriteria(criteria)) {
            List<Book> books = bookRepository.findAll(pageable.getPageNumber(), pageable.getPageSize());
            long total = bookRepository.count();
            Page<Book> page = new PageImpl<>(books, pageable, total);
            return page.map(bookMapper::toResponse);
        }

        Set<String> expandedGenres = Set.of();
        if (criteria.getGenres() != null && !criteria.getGenres().isEmpty()) {
            expandedGenres = NormalizationUtils.expandMatchingGenres(
                    criteria.getGenres(),
                    bookRepository.findAllGenres()
            );
            if (expandedGenres.isEmpty()) {
                return Page.empty(pageable);
            }
        }

        Optional<BookSearchPage> esPage = bookSearchPort.filterBooks(
                criteria,
                expandedGenres,
                pageable.getPageNumber(),
                pageable.getPageSize()
        );
        if (esPage.isEmpty()) {
            log.warn("Search backend filter unavailable, falling back to DB filter");
            Page<Book> books = bookRepository.filterBooks(criteria, pageable);
            return books.map(bookMapper::toResponse);
        }
        return toBookResponsePage(esPage.get(), pageable);
    }

    public List<String> getAllGenres() {
        return NormalizationUtils.dedupeGenreLabels(bookRepository.findAllGenres());
    }

    private Page<BookResponse> toBookResponsePage(BookSearchPage searchPage, Pageable pageable) {
        List<BookResponse> books = searchPage.bookIds().stream()
                .map(id -> bookRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .map(bookMapper::toResponse)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        return new PageImpl<>(books, pageable, searchPage.totalHits());
    }

    private boolean isEmptyCriteria(BookFilterCriteria criteria) {
        return (criteria.getGenres() == null || criteria.getGenres().isEmpty())
                && criteria.getMinRating() == null
                && (criteria.getPacing() == null || criteria.getPacing().isEmpty())
                && criteria.getYearFrom() == null
                && criteria.getYearTo() == null
                && !Boolean.TRUE.equals(criteria.getContentSafe())
                && (criteria.getSearchQuery() == null || criteria.getSearchQuery().isBlank());
    }
}
