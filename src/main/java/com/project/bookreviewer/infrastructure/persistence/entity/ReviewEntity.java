package com.project.bookreviewer.infrastructure.persistence.entity;

import com.project.bookreviewer.domain.model.Pacing;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "book_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "book_id", nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private Integer rating;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String verdict;

    @Column(columnDefinition = "TEXT")
    private String detailedReview;

    @Enumerated(EnumType.STRING)
    private Pacing pacing;

    @ElementCollection
    @CollectionTable(name = "review_moods", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "mood")
    private Set<String> mood;

    @Column(columnDefinition = "TEXT")
    private String whoIsItFor;

    @Column(columnDefinition = "TEXT")
    private String whoIsItNotFor;

    @ElementCollection
    @CollectionTable(name = "review_content_warnings", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "warning")
    private Set<String> contentWarnings;

    @Column(columnDefinition = "TEXT")
    private String spoilerContent;

    private Boolean hasSpoiler;

    @ElementCollection
    @CollectionTable(name = "review_tags", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "tag")
    private Set<String> tags;

    @Builder.Default
    private Integer helpfulCount = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
