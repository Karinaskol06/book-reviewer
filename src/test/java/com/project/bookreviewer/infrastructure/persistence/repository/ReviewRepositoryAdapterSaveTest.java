package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.infrastructure.persistence.entity.ReviewEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewRepositoryAdapterSaveTest {

    @Mock
    private JpaReviewRepository jpaReviewRepository;

    @InjectMocks
    private ReviewRepositoryAdapter adapter;

    @Test
    void save_mapsCreatedAtOntoEntity_soUpdatesDoNotWipeIt() {
        LocalDateTime createdAt = LocalDateTime.of(2024, 3, 15, 10, 30);
        Review review = Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(4)
                .verdict("Updated verdict")
                .helpfulCount(3)
                .createdAt(createdAt)
                .build();

        when(jpaReviewRepository.save(any(ReviewEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        adapter.save(review);

        ArgumentCaptor<ReviewEntity> entityCaptor = ArgumentCaptor.forClass(ReviewEntity.class);
        verify(jpaReviewRepository).save(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getCreatedAt()).isEqualTo(createdAt);
    }
}
