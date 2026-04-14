package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.application.service.BookFilterService;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.domain.model.BookFilterCriteria;
import com.project.bookreviewer.domain.model.Pacing;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class BookFilterController {
    private final BookFilterService filterService;
    private final BookMapper bookMapper;

    @GetMapping("/books/filter")
    public ResponseEntity<Page<BookResponse>> filterBooks(
            @RequestParam(required = false) Set<String> genres,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Set<String> pacing,
            @RequestParam(required = false) Integer yearFrom,
            @RequestParam(required = false) Integer yearTo,
            @RequestParam(required = false) Boolean contentSafe,
            @RequestParam(required = false) String query,
            @PageableDefault(size = 12) Pageable pageable) {

        Set<Pacing> pacingEnums = null;
        if (pacing != null && !pacing.isEmpty()) {
            pacingEnums = pacing.stream()
                    .map(String::toUpperCase)
                    .map(Pacing::valueOf)
                    .collect(Collectors.toSet());
        }

        BookFilterCriteria criteria = BookFilterCriteria.builder()
                .genres(genres)
                .minRating(minRating)
                .pacing(pacingEnums)
                .yearFrom(yearFrom)
                .yearTo(yearTo)
                .contentSafe(contentSafe)
                .searchQuery(query)
                .build();

        Page<Book> books = filterService.filterBooks(criteria, pageable);
        return ResponseEntity.ok(books.map(bookMapper::toResponse));
    }

    @GetMapping("/genres")
    public ResponseEntity<List<String>> getAllGenres() {
        return ResponseEntity.ok(filterService.getAllGenres());
    }
}