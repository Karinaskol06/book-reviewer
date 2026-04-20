package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ClubMembership {
    private Long id;
    private Long clubId;
    private Long userId;
    private ClubRole role;
    private ClubMembershipStatus status;
    private LocalDateTime joinedAt;
}