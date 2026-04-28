package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.CreateBookRequest;
import com.project.bookreviewer.application.dto.response.BookDetailResponse;
import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.application.dto.response.RatingStatsDto;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.application.service.BookService;
import com.project.bookreviewer.application.service.ReviewService;
import com.project.bookreviewer.application.service.SearchService;
import com.project.bookreviewer.application.service.UserBookStatusService;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/books")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;
    private final ReviewService reviewService;
    private final SearchService searchService;
    private final UserBookStatusService userBookStatusService;
    private final SecurityUtils securityUtils;
    private final BookMapper bookMapper;

    @GetMapping
    public ResponseEntity<List<BookResponse>> getBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String genre) {
        List<BookResponse> books;
        if (genre != null) {
            books = bookService.getBooksByGenre(genre, page, size)
                    .stream().map(bookMapper::toResponse).collect(Collectors.toList());
        } else {
            books = bookService.getBooks(page, size)
                    .stream().map(bookMapper::toResponse).collect(Collectors.toList());
        }
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDetailResponse> getBook(@PathVariable Long id) {
        Book book = bookService.getBook(id);
        BookDetailResponse response = bookMapper.toDetailResponse(book);

        response.setRatingStats(reviewService.getRatingStatsDto(id));

        if (securityUtils.isAuthenticated()) {
            Long userId = securityUtils.getCurrentUserId();
            userBookStatusService.getStatus(userId, id)
                    .ifPresent(status -> response.setUserReadingStatus(status.getStatus()));
            response.setUserHasReviewed(reviewService.hasUserReviewed(userId, id));
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<BookResponse> createBook(@Valid @RequestBody CreateBookRequest request) {
        Long actorUserId = securityUtils.isAuthenticated() ? securityUtils.getCurrentUserId() : null;
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .coverUrl(request.getCoverUrl())
                .publicationYear(request.getPublicationYear())
                .genres(request.getGenres())
                .build();
        Book created = bookService.createBook(book, actorUserId);
        return ResponseEntity.created(URI.create("/api/books/" + created.getId()))
                .body(bookMapper.toResponse(created));
    }

    @GetMapping("/check")
    public ResponseEntity<DuplicateCheckResponse> checkDuplicate(
            @RequestParam String title,
            @RequestParam String author) {
        return ResponseEntity.ok(bookService.checkDuplicate(title, author));
    }

    @GetMapping("/search")
    public ResponseEntity<Page<BookResponse>> search(
            @RequestParam String query,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(searchService.searchBooks(query, pageable));
    }

}
