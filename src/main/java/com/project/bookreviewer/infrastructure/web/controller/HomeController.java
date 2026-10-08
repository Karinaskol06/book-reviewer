package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.mapper.BookMapper;
import com.project.bookreviewer.application.service.BookService;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {
    private final BookService bookService;
    private final BookMapper bookMapper;
    private final SecurityUtils securityUtils;

    @GetMapping("/trending")
    public ResponseEntity<List<BookResponse>> getTrending(
            @RequestParam(defaultValue = "8") int limit) {
        Long excludeUserId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(
                bookService.getTrendingBooks(limit, excludeUserId)
                        .stream().map(bookMapper::toResponse).collect(Collectors.toList())
        );
    }
}
