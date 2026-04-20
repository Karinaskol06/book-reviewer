package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Builder
@Data
public class UpdateClubRequest {
    private String name;
    private String description;
    private String focus;
    private Long currentBookId;
    private Boolean isPrivate;
    private String coverImageUrl;
    private LocalDateTime nextMeetingAt;
}
