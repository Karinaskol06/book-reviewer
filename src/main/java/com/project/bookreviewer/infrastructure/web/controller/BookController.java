package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.CreateBookRequest;
import com.project.bookreviewer.application.dto.response.BookDetailResponse;
import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.dto.response.DuplicateCheckResponse;
import com.project.bookreviewer.application.dto.response.RatingStatsDto;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.application.service.BookService;
import com.project.bookreviewer.application.service.ReviewService;
import com.project.bookreviewer.application.service.UserBookStatusService;
import com.project.bookreviewer.domain.model.Book;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
        Book book = Book.builder()
                .title(request.getTitle())
                .author(request.getAuthor())
                .description(request.getDescription())
                .coverUrl(request.getCoverUrl())
                .publicationYear(request.getPublicationYear())
                .genres(request.getGenres())
                .build();
        Book created = bookService.createBook(book);
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
    public ResponseEntity<List<BookResponse>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                bookService.searchBooks(query, page, size).stream()
                        .map(bookMapper::toResponse).collect(Collectors.toList())
        );
    }

    /* helper methods */

    private RatingStatsDto buildRatingStats(Object[] stats) {
        if (stats == null || stats.length < 7) {
            return RatingStatsDto.builder()
                    .average(0.0)
                    .total(0)
                    .distribution(Map.of(5,0,4,0,3,0,2,0,1,0))
                    .build();
        }

        Double avg = stats[0] != null ? (Double) stats[0] : 0.0;
        Long total = stats[1] != null ? (Long) stats[1] : 0L;
        Long count5 = stats[2] != null ? (Long) stats[2] : 0L;
        Long count4 = stats[3] != null ? (Long) stats[3] : 0L;
        Long count3 = stats[4] != null ? (Long) stats[4] : 0L;
        Long count2 = stats[5] != null ? (Long) stats[5] : 0L;
        Long count1 = stats[6] != null ? (Long) stats[6] : 0L;

        Map<Integer, Integer> distribution = Map.of(
                5, count5.intValue(),
                4, count4.intValue(),
                3, count3.intValue(),
                2, count2.intValue(),
                1, count1.intValue()
        );

        return RatingStatsDto.builder()
                .average(avg)
                .total(total.intValue())
                .distribution(distribution)
                .build();
    }


}
