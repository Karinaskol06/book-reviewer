package com.project.bookreviewer.application.dto.response;


import com.project.bookreviewer.domain.model.ReadingStatus;
import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class BookResponse {
    private Long id;
    private String title;
    private String author;
    private String description;
    private String coverUrl;
    private Integer publicationYear;
    private Set<String> genres;
    private Double averageRating;   // will be populated later
    private RatingStatsDto ratingStats;
    private ReadingStatus userReadingStatus; // if authenticated
    private Boolean userHasReviewed; // if authenticated
}
