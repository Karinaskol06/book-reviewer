package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class ReviewSnippetDto {
    private Long id;
    private String verdict;
    private Integer rating;
    private String detailedReview;
    private String whoIsItFor;
    private Set<String> mood;
    private Integer helpfulCount;
}