package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class User {
    private Long id;
    private String username;
    private String email;
    private String password; // hashed
    private String avatarUrl;
    private Set<Role> roles;
    private LocalDateTime createdAt;
    private boolean enabled;
}
