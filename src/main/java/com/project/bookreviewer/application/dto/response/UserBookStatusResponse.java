package com.project.bookreviewer.application.dto.response;

import com.project.bookreviewer.domain.model.ReadingStatus;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class UserBookStatusResponse {
    private Long id;
    private Long userId;
    private Long bookId;
    private ReadingStatus status;
    private LocalDateTime updatedAt;
}