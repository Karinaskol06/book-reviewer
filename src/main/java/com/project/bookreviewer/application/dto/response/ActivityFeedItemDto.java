package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ActivityFeedItemDto {
    private Long id;
    private ReviewResponse.ReviewUserDto actor;
    private String type; // e.g., "REVIEWED", "WANT_TO_READ"
    private BookSummaryDto book;
    private Long reviewId;
    private Integer rating; // if review
    private LocalDateTime createdAt;
    private ReviewSnippetDto reviewSnippet;
}