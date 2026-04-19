package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.ActivityFeedItemDto;
import com.project.bookreviewer.application.service.FeedService;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
public class FeedController {
    private final FeedService feedService;
    private final SecurityUtils securityUtils;

    @GetMapping
    public ResponseEntity<Page<ActivityFeedItemDto>> getFeed(@PageableDefault(size = 20) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(feedService.getUserFeed(userId, pageable));
    }
}