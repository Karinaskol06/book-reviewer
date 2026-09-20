package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ReviewHelpful;
import com.project.bookreviewer.domain.port.outbound.ReviewHelpfulRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewHelpfulEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ReviewHelpfulRepositoryAdapter implements ReviewHelpfulRepositoryPort {
    private final JpaReviewHelpfulRepository jpaRepo;

    @Override
    public ReviewHelpful save(ReviewHelpful helpful) {
        ReviewHelpfulEntity saved = jpaRepo.save(mapToEntity(helpful));
        return mapToDomain(saved);
    }

    @Override
    public Optional<ReviewHelpful> findByReviewIdAndUserId(Long reviewId, Long userId) {
        return jpaRepo.findByReviewIdAndUserId(reviewId, userId).map(this::mapToDomain);
    }

    @Override
    @Transactional
    public void deleteByReviewIdAndUserId(Long reviewId, Long userId) {
        jpaRepo.deleteByReviewIdAndUserId(reviewId, userId);
    }

    @Override
    public boolean existsByReviewIdAndUserId(Long reviewId, Long userId) {
        return jpaRepo.existsByReviewIdAndUserId(reviewId, userId);
    }

    @Override
    @Transactional
    public void deleteByReviewId(Long reviewId) {
        jpaRepo.deleteByReviewId(reviewId);
    }

    private ReviewHelpfulEntity mapToEntity(ReviewHelpful domain) {
        return ReviewHelpfulEntity.builder()
                .id(domain.getId())
                .reviewId(domain.getReviewId())
                .userId(domain.getUserId())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    private ReviewHelpful mapToDomain(ReviewHelpfulEntity entity) {
        return ReviewHelpful.builder()
                .id(entity.getId())
                .reviewId(entity.getReviewId())
                .userId(entity.getUserId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
