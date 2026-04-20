package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.PostInsightful;

import java.util.Optional;

public interface PostInsightfulRepositoryPort {
    PostInsightful save(PostInsightful insightful);
    Optional<PostInsightful> findByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    boolean existsByPostIdAndUserId(Long postId, Long userId);
    void deleteByPostId(Long postId);
}