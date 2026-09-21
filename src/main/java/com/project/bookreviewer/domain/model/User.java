package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Getter
@Builder
public class User {
    private Long id;
    private String username;
    private String email;
    private String password; // hashed
    private String avatarUrl;
    private String displayName;
    private String aboutMe;
    private List<String> socialLinks;
    private Set<Role> roles;
    private LocalDateTime createdAt;
    private boolean enabled;
}
