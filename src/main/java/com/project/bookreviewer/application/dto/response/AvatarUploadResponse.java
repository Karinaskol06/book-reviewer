package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvatarUploadResponse {
    private String avatarUrl;
    private String message;
}
