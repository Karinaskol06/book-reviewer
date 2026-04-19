package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewSnippetDto {
    private Long id;
    private String verdict;
    private Integer rating;
}