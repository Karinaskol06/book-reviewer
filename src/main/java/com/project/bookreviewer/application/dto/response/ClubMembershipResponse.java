package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ClubMembershipResponse {
    private Long id;
    private Long clubId;
    private ReviewResponse.ReviewUserDto user;
    private String role;
    private String status;
    private LocalDateTime joinedAt;
}