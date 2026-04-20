package com.project.bookreviewer.infrastructure.persistence.entity;

import com.project.bookreviewer.domain.model.ClubMembershipStatus;
import com.project.bookreviewer.domain.model.ClubRole;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "club_memberships", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"club_id", "user_id"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClubMembershipEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_id", nullable = false)
    private Long clubId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClubMembershipStatus status;

    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    @Column(name = "invited_by")
    private Long invitedBy;

    @PrePersist
    protected void onCreate() {
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
    }
}