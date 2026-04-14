package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class UserBookStatus {
    private Long id;
    private Long userId;
    private Long bookId;
    private ReadingStatus status;
    private LocalDateTime updatedAt;
}