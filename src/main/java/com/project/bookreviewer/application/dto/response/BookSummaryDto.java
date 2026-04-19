package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class BookSummaryDto {
    private Long id;
    private String title;
    private String author;
    private String coverUrl;
}