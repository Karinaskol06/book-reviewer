package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class Follow {
    private Long id;
    private Long followerId;
    private Long followingId;
    private LocalDateTime createdAt;
}