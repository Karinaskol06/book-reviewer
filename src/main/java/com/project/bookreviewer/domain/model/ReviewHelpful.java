package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewHelpful {
    private Long id;
    private Long reviewId;
    private Long userId;
    private LocalDateTime createdAt;
}
