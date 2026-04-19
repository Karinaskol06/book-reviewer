package com.project.bookreviewer.domain.port.outbound;

import com.project.bookreviewer.domain.model.Follow;
import java.util.List;
import java.util.Optional;

public interface FollowRepositoryPort {
    Follow save(Follow follow);
    void delete(Long followerId, Long followingId);
    Optional<Follow> findByFollowerAndFollowing(Long followerId, Long followingId);
    List<Follow> findFollowers(Long userId);
    List<Follow> findFollowing(Long userId);
    boolean existsByFollowerAndFollowing(Long followerId, Long followingId);
    long countFollowers(Long userId);
    long countFollowing(Long userId);
}