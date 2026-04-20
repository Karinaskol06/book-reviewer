package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ClubResponse {
    private Long id;
    private String name;
    private String description;
    private String focus;
    private BookSummaryDto currentBook;
    private Boolean isPrivate;
    private String coverImageUrl;
    private LocalDateTime nextMeetingAt;
    private ReviewResponse.ReviewUserDto owner;
    private Long memberCount;
    private Long pendingCount;
    private ClubMembershipResponse userMembership;
    private LocalDateTime createdAt;
}