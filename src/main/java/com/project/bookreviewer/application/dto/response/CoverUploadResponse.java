package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CoverUploadResponse {
    private String coverUrl;
    private String message;
}
