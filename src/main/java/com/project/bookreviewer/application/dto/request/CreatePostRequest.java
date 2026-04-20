package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePostRequest {
    private Long parentPostId; // null for top-level post
    @NotBlank
    private String content;
}