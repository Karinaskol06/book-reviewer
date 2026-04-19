package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.domain.port.outbound.FollowRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.FollowEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class FollowRepositoryAdapter implements FollowRepositoryPort {
    private final JpaFollowRepository jpaFollowRepository;

    @Override
    public Follow save(Follow follow) {
        FollowEntity entity = FollowEntity.builder()
                .followerId(follow.getFollowerId())
                .followingId(follow.getFollowingId())
                .build();
        FollowEntity saved = jpaFollowRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public void delete(Long followerId, Long followingId) {
        jpaFollowRepository.deleteByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    public Optional<Follow> findByFollowerAndFollowing(Long followerId, Long followingId) {
        return jpaFollowRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .map(this::mapToDomain);
    }

    @Override
    public List<Follow> findFollowers(Long userId) {
        return jpaFollowRepository.findByFollowingId(userId).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public List<Follow> findFollowing(Long userId) {
        return jpaFollowRepository.findByFollowerId(userId).stream()
                .map(this::mapToDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByFollowerAndFollowing(Long followerId, Long followingId) {
        return jpaFollowRepository.existsByFollowerIdAndFollowingId(followerId, followingId);
    }

    @Override
    public long countFollowers(Long userId) {
        return jpaFollowRepository.countByFollowingId(userId);
    }

    @Override
    public long countFollowing(Long userId) {
        return jpaFollowRepository.countByFollowerId(userId);
    }

    private Follow mapToDomain(FollowEntity entity) {
        return Follow.builder()
                .id(entity.getId())
                .followerId(entity.getFollowerId())
                .followingId(entity.getFollowingId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}