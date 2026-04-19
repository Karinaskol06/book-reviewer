package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.FollowStats;
import com.project.bookreviewer.application.service.FollowService;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class FollowController {
    private final FollowService followService;
    private final SecurityUtils securityUtils;

    @PostMapping("/{userId}/follow")
    public ResponseEntity<Void> follow(@PathVariable Long userId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        followService.follow(currentUserId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{userId}/follow")
    public ResponseEntity<Void> unfollow(@PathVariable Long userId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        followService.unfollow(currentUserId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{userId}/follow")
    public ResponseEntity<Boolean> isFollowing(@PathVariable Long userId) {
        Long currentUserId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(followService.isFollowing(currentUserId, userId));
    }

    @GetMapping("/{userId}/follow-stats")
    public ResponseEntity<FollowStats> getStats(@PathVariable Long userId) {
        return ResponseEntity.ok(followService.getStats(userId));
    }
}