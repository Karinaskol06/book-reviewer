package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PostInsightful {
    private Long id;
    private Long postId;
    private Long userId;
    private LocalDateTime createdAt;
}