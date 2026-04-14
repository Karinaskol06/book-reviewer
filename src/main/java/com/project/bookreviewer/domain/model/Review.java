package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class Review {
    private Long id;
    private Long userId;
    private Long bookId;
    private Integer rating; // 1-5
    private String verdict; // required short summary
    private String detailedReview;
    private Pacing pacing;
    private Set<String> mood; // e.g., ["Reflective","Hopeful"]
    private String whoIsItFor;
    private String whoIsItNotFor;
    private Set<String> contentWarnings;
    private String spoilerContent;
    private Boolean hasSpoiler;
    private Set<String> tags;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
