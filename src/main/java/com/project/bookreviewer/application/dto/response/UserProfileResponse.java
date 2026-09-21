package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private String aboutMe;
    @Builder.Default
    private List<String> socialLinks = new ArrayList<>();
    private LocalDateTime joinedAt;
    private Set<String> roles;
    private Integer booksReviewed;
    private Integer booksWantToRead;
    private Integer booksReading;
    private Integer booksRead;
}
