package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.CreatePostRequest;
import com.project.bookreviewer.application.dto.response.ClubPostResponse;
import com.project.bookreviewer.domain.port.inbound.ClubDiscussionUseCase;
import com.project.bookreviewer.domain.model.ClubPost;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ClubDiscussionController {
    private final ClubDiscussionUseCase discussionUseCase;
    private final SecurityUtils securityUtils;

    // Posts

    @PostMapping("/{clubId}/posts")
    public ResponseEntity<ClubPostResponse> createPost(
            @PathVariable Long clubId,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ClubPost post = discussionUseCase.createPost(clubId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionUseCase.getPost(post.getId(), userId));
    }

    @GetMapping("/{clubId}/posts")
    public ResponseEntity<Page<ClubPostResponse>> getClubPosts(
            @PathVariable Long clubId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(discussionUseCase.getClubPosts(clubId, pageable, userId));
    }

    @GetMapping("/{clubId}/posts/{postId}")
    public ResponseEntity<ClubPostResponse> getPost(
            @PathVariable Long clubId,
            @PathVariable Long postId) {
        Long userId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(discussionUseCase.getPost(postId, userId));
    }

    @PutMapping("/{clubId}/posts/{postId}")
    public ResponseEntity<ClubPostResponse> updatePost(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ClubPost post = discussionUseCase.updatePost(postId, userId, request.getContent());
        return ResponseEntity.ok(discussionUseCase.getPost(post.getId(), userId));
    }

    @DeleteMapping("/{clubId}/posts/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long clubId,
            @PathVariable Long postId) {
        Long userId = securityUtils.getCurrentUserId();
        discussionUseCase.deletePost(postId, userId);
        return ResponseEntity.noContent().build();
    }

    // Replies

    @PostMapping("/{clubId}/posts/{postId}/replies")
    public ResponseEntity<ClubPostResponse> createReply(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @Valid @RequestBody CreatePostRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        request.setParentPostId(postId);
        ClubPost reply = discussionUseCase.createPost(clubId, userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(discussionUseCase.getPost(reply.getId(), userId));
    }

    @GetMapping("/{clubId}/posts/{postId}/replies")
    public ResponseEntity<Page<ClubPostResponse>> getPostReplies(
            @PathVariable Long clubId,
            @PathVariable Long postId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(discussionUseCase.getPostReplies(postId, pageable, userId));
    }

    // Insightful

    @PostMapping("/{clubId}/posts/{postId}/insightful")
    public ResponseEntity<Void> toggleInsightful(
            @PathVariable Long clubId,
            @PathVariable Long postId) {
        Long userId = securityUtils.getCurrentUserId();
        discussionUseCase.toggleInsightful(postId, userId);
        return ResponseEntity.ok().build();
    }
}