package com.project.bookreviewer.domain.model;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class ActivityEvent {
    private Long id;
    private Long actorId;        // user who performed the action
    private Long targetUserId;   // user whose feed this event belongs to (can be same as actor for self-feed)
    private ActivityType type;
    private Long bookId;         // optional, related book
    private Long reviewId;       // optional
    private String additionalData; // JSON string for extra context
    private LocalDateTime createdAt;

    /**
     * {@code additionalData} for a FOLLOWED_USER card: the user who was followed. Kept in one place so
     * the writer and the unfollow cleanup always produce the same string.
     */
    public static String followedUserData(Long followedUserId) {
        return "{\"targetUserId\": " + followedUserId + "}";
    }
}

