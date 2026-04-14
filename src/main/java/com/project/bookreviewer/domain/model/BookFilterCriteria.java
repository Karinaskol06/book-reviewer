package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.util.Set;

@Getter
@Builder
public class BookFilterCriteria {
    private Set<String> genres;
    private Integer minRating;
    private Set<Pacing> pacing;
    private Integer yearFrom;
    private Integer yearTo;
    private Boolean contentSafe; // if true, exclude books with certain warnings
    private String searchQuery; // optional full-text
}