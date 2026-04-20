package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ReadingClub {
    private Long id;
    private String name;
    private String description;
    private String focus;
    private Long currentBookId;
    private Boolean isPrivate;
    private String coverImageUrl;
    private LocalDateTime nextMeetingAt;
    private Long createdBy;
    private LocalDateTime createdAt;
}