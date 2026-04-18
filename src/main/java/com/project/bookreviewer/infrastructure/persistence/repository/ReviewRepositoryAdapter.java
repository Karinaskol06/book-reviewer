package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryAdapter implements ReviewRepositoryPort {
    private final JpaReviewRepository jpaReviewRepository;

    @Override
    public Review save(Review review) {
        ReviewEntity entity = mapToEntity(review);
        ReviewEntity saved = jpaReviewRepository.save(entity);
        return mapToDomain(saved);
    }

    @Override
    public Optional<Review> findById(Long id) {
        return jpaReviewRepository.findById(id).map(this::mapToDomain);
    }

    @Override
    public Optional<Long> findUserIdByReviewId(Long reviewId) {
        return jpaReviewRepository.findUserIdByReviewId(reviewId);
    }

    @Override
    public Page<Review> findByBookId(Long bookId, Pageable pageable) {
        return jpaReviewRepository.findByBookId(bookId, pageable)
                .map(this::mapToDomain);
    }

    @Override
    public Optional<Review> findByUserIdAndBookId(Long userId, Long bookId) {
        return jpaReviewRepository.findByUserIdAndBookId(userId, bookId)
                .map(this::mapToDomain);
    }

    @Override
    public void deleteById(Long id) {
        jpaReviewRepository.deleteById(id);
    }

    @Override
    public boolean existsByUserIdAndBookId(Long userId, Long bookId) {
        return jpaReviewRepository.existsByUserIdAndBookId(userId, bookId);
    }

    @Override
    public long countByUserId(Long userId) {
        return jpaReviewRepository.countByUserId(userId);
    }

    @Override
    public Object[] getRatingStats(Long bookId) {
        Object[] stats = jpaReviewRepository.getRatingStats(bookId);

        // Hibernate/JPA providers can return aggregation rows as nested Object[].
        // Normalize shape at infrastructure boundary so domain/application always gets flat stats.
        if (stats != null && stats.length == 1 && stats[0] instanceof Object[] nestedStats) {
            return nestedStats;
        }

        return stats;
    }

    @Override
    public List<Object[]> findPacingCountsByBookId(Long bookId) {
        return jpaReviewRepository.findPacingCountsByBookId(bookId);
    }

    @Override
    public List<String> findTopMoodsByBookId(Long bookId, int limit) {
        // Get all moods and manually count frequency, then take top N
        List<String> allMoods = jpaReviewRepository.findAllMoodsByBookId(bookId);
        return allMoods.stream()
                .collect(Collectors.groupingBy(m -> m, Collectors.counting()))
                .entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsContentWarningsByBookId(Long bookId) {
        return jpaReviewRepository.existsContentWarningsByBookId(bookId);
    }

    // Mapping methods
    private ReviewEntity mapToEntity(Review review) {
        return ReviewEntity.builder()
                .id(review.getId())
                .userId(review.getUserId())
                .bookId(review.getBookId())
                .rating(review.getRating())
                .verdict(review.getVerdict())
                .detailedReview(review.getDetailedReview())
                .pacing(review.getPacing())
                .mood(review.getMood())
                .whoIsItFor(review.getWhoIsItFor())
                .whoIsItNotFor(review.getWhoIsItNotFor())
                .contentWarnings(review.getContentWarnings())
                .spoilerContent(review.getSpoilerContent())
                .hasSpoiler(review.getHasSpoiler())
                .tags(review.getTags())
                .helpfulCount(review.getHelpfulCount())
                .build();
    }

    private Review mapToDomain(ReviewEntity entity) {
        return Review.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .bookId(entity.getBookId())
                .rating(entity.getRating())
                .verdict(entity.getVerdict())
                .detailedReview(entity.getDetailedReview())
                .pacing(entity.getPacing())
                .mood(copySet(entity.getMood()))
                .whoIsItFor(entity.getWhoIsItFor())
                .whoIsItNotFor(entity.getWhoIsItNotFor())
                .contentWarnings(copySet(entity.getContentWarnings()))
                .spoilerContent(entity.getSpoilerContent())
                .hasSpoiler(entity.getHasSpoiler())
                .tags(copySet(entity.getTags()))
                .helpfulCount(entity.getHelpfulCount())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Set<String> copySet(Set<String> source) {
        return source == null ? null : new LinkedHashSet<>(source);
    }
}