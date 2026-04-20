package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ClubPost;
import com.project.bookreviewer.domain.port.outbound.ClubPostRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ClubPostEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ClubPostRepositoryAdapter implements ClubPostRepositoryPort {
    private final JpaClubPostRepository jpaRepo;

    @Override
    public ClubPost save(ClubPost post) {
        ClubPostEntity entity = mapToEntity(post);
        ClubPostEntity saved = jpaRepo.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<ClubPost> findById(Long id) {
        return jpaRepo.findById(id).map(this::mapToDomain);
    }

    @Override
    public Page<ClubPost> findByClubId(Long clubId, Pageable pageable) {
        return jpaRepo.findByClubIdOrderByCreatedAtDesc(clubId, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Page<ClubPost> findByClubIdAndParentPostIdIsNull(Long clubId, Pageable pageable) {
        return jpaRepo.findTopLevelPostsByClubId(clubId, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public List<ClubPost> findByParentPostId(Long parentPostId) {
        return jpaRepo.findByParentPostIdOrderByCreatedAtAsc(parentPostId).stream()
                .map(this::mapToDomain)
                .collect(Collectors.toList());
    }

    @Override
    public Page<ClubPost> findByAuthorId(Long authorId, Pageable pageable) {
        return jpaRepo.findByAuthorIdOrderByCreatedAtDesc(authorId, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaRepo.deleteById(id);
    }

    @Override
    public void deleteByClubId(Long clubId) {
        jpaRepo.deleteByClubId(clubId);
    }

    @Override
    public long countByClubId(Long clubId) {
        return jpaRepo.countByClubId(clubId);
    }

    @Override
    public long countRepliesByPostId(Long postId) {
        return jpaRepo.countByParentPostId(postId);
    }

    private ClubPostEntity mapToEntity(ClubPost domain) {
        return ClubPostEntity.builder()
                .id(domain.getId())
                .clubId(domain.getClubId())
                .authorId(domain.getAuthorId())
                .parentPostId(domain.getParentPostId())
                .content(domain.getContent())
                .insightfulCount(domain.getInsightfulCount())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private ClubPost mapToDomain(ClubPostEntity entity) {
        return ClubPost.builder()
                .id(entity.getId())
                .clubId(entity.getClubId())
                .authorId(entity.getAuthorId())
                .parentPostId(entity.getParentPostId())
                .content(entity.getContent())
                .insightfulCount(entity.getInsightfulCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}