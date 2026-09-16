package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.BookResponse;
import com.project.bookreviewer.application.service.RecommendationService;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {
    private final RecommendationService recommendationService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<List<BookResponse>> getRecommendations(
            @RequestParam(defaultValue = "6") int limit) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(recommendationService.getRecommendations(userId, limit));
    }
}
