package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchItemDto {
    private Long id;
    private String username;
    private String avatarUrl;
    private boolean following;
}
