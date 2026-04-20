package com.project.bookreviewer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "club_posts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClubPostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "parent_post_id")
    private Long parentPostId; // null for top-level posts, non-null for replies

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "insightful_count")
    private Integer insightfulCount = 0;

    @Column(name = "is_pinned")
    private Boolean isPinned = false;

    @Column(name = "is_edited")
    private Boolean isEdited = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        isEdited = true;
    }
}