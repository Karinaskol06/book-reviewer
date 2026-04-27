package com.project.bookreviewer.application.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String aboutMe;
    private Set<String> roles;
    private Integer booksReviewed;
    private Integer booksWantToRead;
    private Integer booksReading;
    private Integer booksRead;
}
