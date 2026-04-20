package com.project.bookreviewer.infrastructure.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "reading_clubs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReadingClubEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;
    private String focus;

    @Column(name = "current_book_id")
    private Long currentBookId;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(name = "cover_image_url")
    private String coverImageUrl;

    @Column(name = "next_meeting_at")
    private LocalDateTime nextMeetingAt;

    @Column(name = "created_by")
    private Long createdBy;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}