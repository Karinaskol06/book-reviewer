package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.request.CreatePostRequest;
import com.project.bookreviewer.application.dto.response.ClubPostResponse;
import com.project.bookreviewer.domain.model.ClubPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Driving port for club discussion posts and replies.
 */
public interface ClubDiscussionUseCase {
    ClubPost createPost(Long clubId, Long authorId, CreatePostRequest request);

    ClubPost updatePost(Long postId, Long authorId, String content);

    void deletePost(Long postId, Long userId);

    ClubPostResponse getPost(Long postId, Long userId);

    Page<ClubPostResponse> getClubPosts(Long clubId, Pageable pageable, Long userId);

    Page<ClubPostResponse> getPostReplies(Long postId, Pageable pageable, Long userId);

    void toggleInsightful(Long postId, Long userId);
}
