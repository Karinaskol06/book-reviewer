package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.infrastructure.persistence.entity.FollowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface JpaFollowRepository extends JpaRepository<FollowEntity, Long> {
    Optional<FollowEntity> findByFollowerIdAndFollowingId(Long followerId, Long followingId);
    List<FollowEntity> findByFollowerId(Long followerId);
    List<FollowEntity> findByFollowingId(Long followingId);
    void deleteByFollowerIdAndFollowingId(Long followerId, Long followingId);
    boolean existsByFollowerIdAndFollowingId(Long followerId, Long followingId);
    long countByFollowingId(Long followingId);
    long countByFollowerId(Long followerId);
}