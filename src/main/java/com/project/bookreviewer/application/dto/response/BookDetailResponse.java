package com.project.bookreviewer.application.dto.response;

import com.project.bookreviewer.domain.model.ReadingStatus;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class BookDetailResponse {
    // Basic book info (mirrors BookResponse)
    private Long id;
    private String title;
    private String author;
    private String description;
    private String coverUrl;
    private Integer publicationYear;
    private Set<String> genres;

    // Enhanced fields
    private RatingStatsDto ratingStats;
    private ReadingStatus userReadingStatus;  // null if not authenticated or no status
    private Boolean userHasReviewed;          // false if not authenticated
}
