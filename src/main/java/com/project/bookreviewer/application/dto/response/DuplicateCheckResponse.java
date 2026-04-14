package com.project.bookreviewer.application.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateCheckResponse {
    private boolean exists;
    private Long bookId;
    private String bookUrl;
    private String title;
    private String author;
}