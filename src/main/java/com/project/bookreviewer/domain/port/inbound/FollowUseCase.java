package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.response.FollowStats;
import com.project.bookreviewer.application.dto.response.UserSearchItemDto;

import java.util.List;

/**
 * Driving port for follow graph operations.
 */
public interface FollowUseCase {
    void follow(Long followerId, Long followingId);

    void unfollow(Long followerId, Long followingId);

    boolean isFollowing(Long followerId, Long followingId);

    FollowStats getStats(Long userId);

    List<UserSearchItemDto> getFollowers(Long userId);

    List<UserSearchItemDto> getFollowing(Long userId);

    List<UserSearchItemDto> searchUsers(Long currentUserId, String query, int limit);
}
