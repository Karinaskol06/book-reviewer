package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ClubPostResponse {
    private Long id;
    private Long clubId;
    private ReviewResponse.ReviewUserDto author;
    private Long parentPostId;
    private String content;
    private Integer insightfulCount;
    private Boolean hasInsightful;
    private Long replyCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}