package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.PostInsightful;
import com.project.bookreviewer.domain.port.outbound.PostInsightfulRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.PostInsightfulEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class PostInsightfulRepositoryAdapter implements PostInsightfulRepositoryPort {
    private final JpaPostInsightfulRepository jpaRepo;

    @Override
    public PostInsightful save(PostInsightful insightful) {
        PostInsightfulEntity entity = mapToEntity(insightful);
        PostInsightfulEntity saved = jpaRepo.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<PostInsightful> findByPostIdAndUserId(Long postId, Long userId) {
        return jpaRepo.findByPostIdAndUserId(postId, userId).map(this::mapToDomain);
    }

    @Override
    public void deleteByPostIdAndUserId(Long postId, Long userId) {
        jpaRepo.deleteByPostIdAndUserId(postId, userId);
    }

    @Override
    public long countByPostId(Long postId) {
        return jpaRepo.countByPostId(postId);
    }

    @Override
    public boolean existsByPostIdAndUserId(Long postId, Long userId) {
        return jpaRepo.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    public void deleteByPostId(Long postId) {
        jpaRepo.deleteByPostId(postId);
    }

    private PostInsightfulEntity mapToEntity(PostInsightful domain) {
        return PostInsightfulEntity.builder()
                .id(domain.getId())
                .postId(domain.getPostId())
                .userId(domain.getUserId())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private PostInsightful mapToDomain(PostInsightfulEntity entity) {
        return PostInsightful.builder()
                .id(entity.getId())
                .postId(entity.getPostId())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}