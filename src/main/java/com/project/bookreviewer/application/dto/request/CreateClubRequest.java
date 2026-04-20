package com.project.bookreviewer.application.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CreateClubRequest {
    @NotBlank
    private String name;
    private String description;
    private String focus;
    private Long currentBookId;
    private Boolean isPrivate = false;
    private String coverImageUrl;
    private LocalDateTime nextMeetingAt;
}