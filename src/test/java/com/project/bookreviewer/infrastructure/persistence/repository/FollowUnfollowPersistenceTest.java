package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.domain.model.Follow;
import com.project.bookreviewer.infrastructure.persistence.entity.ActivityEventEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.FollowEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({FollowRepositoryAdapter.class, ActivityEventRepositoryAdapter.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:followdelete;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class FollowUnfollowPersistenceTest {

    @Autowired
    private FollowRepositoryAdapter followRepositoryAdapter;
    @Autowired
    private ActivityEventRepositoryAdapter activityEventRepositoryAdapter;
    @Autowired
    private JpaFollowRepository jpaFollowRepository;
    @Autowired
    private JpaActivityEventRepository jpaActivityEventRepository;

    @Test
    @Transactional
    void delete_removesFollowRow() {
        jpaFollowRepository.save(FollowEntity.builder()
                .followerId(1L)
                .followingId(2L)
                .build());
        assertThat(jpaFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).isTrue();

        followRepositoryAdapter.delete(1L, 2L);

        assertThat(jpaFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).isFalse();
    }

    @Test
    @Transactional
    void unfollowStyleCleanup_removesFollowAndFeedEvents() {
        jpaFollowRepository.save(FollowEntity.builder()
                .followerId(1L)
                .followingId(2L)
                .build());
        jpaActivityEventRepository.save(ActivityEventEntity.builder()
                .actorId(2L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(10L)
                .reviewId(99L)
                .createdAt(LocalDateTime.now().minusDays(1))
                .build());

        // Same order as FollowService.unfollow
        followRepositoryAdapter.delete(1L, 2L);
        activityEventRepositoryAdapter.deleteByActorIdAndTargetUserId(2L, 1L);

        assertThat(jpaFollowRepository.existsByFollowerIdAndFollowingId(1L, 2L)).isFalse();
        assertThat(jpaActivityEventRepository.existsByReviewIdAndTargetUserId(99L, 1L)).isFalse();
        assertThat(followRepositoryAdapter.existsByFollowerAndFollowing(1L, 2L)).isFalse();
    }

    @Test
    @Transactional
    void save_roundTripsFollow() {
        Follow saved = followRepositoryAdapter.save(Follow.builder()
                .followerId(3L)
                .followingId(4L)
                .build());
        assertThat(saved.getId()).isNotNull();
        assertThat(followRepositoryAdapter.existsByFollowerAndFollowing(3L, 4L)).isTrue();
    }
}
