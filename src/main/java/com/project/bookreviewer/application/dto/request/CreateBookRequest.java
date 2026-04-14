package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.util.Set;

@Data
public class CreateBookRequest {
    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Author is required")
    private String author;

    private String description;
    private String coverUrl;
    private Integer publicationYear;
    private Set<String> genres;
}