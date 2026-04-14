package com.project.bookreviewer.domain.model;

import com.project.bookreviewer.shared.util.NormalizationUtils;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class Book {
    private Long id;
    private String title;
    private String normalizedTitle;
    private String author;
    private String description;
    private String coverUrl;
    private Integer publicationYear;
    private Set<String> genres;
    private LocalDateTime createdAt;

    // Caching rating stats
    private Double averageRating;
    private Integer ratingCount;
    private Integer totalReviews;

    public void normalizeFields() {
        this.normalizedTitle = NormalizationUtils.normalize(title);
    }
}