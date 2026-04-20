package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ClubPost {
    private Long id;
    private Long clubId;
    private Long authorId;
    private Long parentPostId;      // null for top-level post, otherwise reply
    private String content;
    private Integer insightfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}