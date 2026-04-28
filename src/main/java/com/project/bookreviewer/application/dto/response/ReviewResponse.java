package com.project.bookreviewer.application.dto.response;

import com.project.bookreviewer.domain.model.Pacing;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
public class ReviewResponse {
    private Long id;
    private Long bookId;
    private ReviewUserDto user;
    private Integer rating;
    private String verdict;
    private String detailedReview;
    private Pacing pacing;
    private Set<String> mood;
    private String whoIsItFor;
    private String whoIsItNotFor;
    private Set<String> contentWarnings;
    private Boolean hasSpoiler;
    private String spoilerContent; // only included if user requests or has revealed
    private Set<String> tags;
    private Integer helpfulCount;
    private LocalDateTime createdAt;

    @Data @Builder
    public static class ReviewUserDto {
        private Long id;
        private String username;
        private String avatarUrl;
        private String badge; // "MASTER REVIEWER" based on review count
        private Integer booksReviewed;
    }
}