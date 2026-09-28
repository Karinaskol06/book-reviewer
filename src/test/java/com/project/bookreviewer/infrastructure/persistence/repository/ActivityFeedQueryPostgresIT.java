package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.infrastructure.persistence.entity.ActivityEventEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.FollowEntity;
import com.project.bookreviewer.infrastructure.persistence.support.AbstractPostgresDataJpaTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real-Postgres coverage for {@code findFeedEvents}:
 * follow EXISTS join, exclude self, ignore events from non-followed actors.
 * Complements the faster H2 {@link ActivityFeedQueryTest}.
 */
@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActivityFeedQueryPostgresIT extends AbstractPostgresDataJpaTest {

    @Autowired
    private JpaActivityEventRepository activityEventRepository;
    @Autowired
    private JpaFollowRepository followRepository;

    @BeforeEach
    void setUp() {
        activityEventRepository.deleteAll();
        followRepository.deleteAll();
    }

    @Test
    void findFeedEvents_includesOnlyEventsFromFollowedUsers_orderedNewestFirst() {
        followRepository.save(FollowEntity.builder()
                .followerId(1L)
                .followingId(2L)
                .build());

        ActivityEventEntity older = activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(2L)
                .targetUserId(1L)
                .type(ActivityType.STARTED_READING)
                .bookId(41L)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        ActivityEventEntity newer = activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(2L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(40L)
                .reviewId(7L)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());

        // noise: targeted at user 1 but actor is not followed
        activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(9L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(50L)
                .reviewId(8L)
                .createdAt(LocalDateTime.now())
                .build());

        // self activity must be excluded (actorId = targetUserId = 1)
        activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(1L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(60L)
                .reviewId(9L)
                .createdAt(LocalDateTime.now())
                .build());

        Page<ActivityEventEntity> page = activityEventRepository.findFeedEvents(1L, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(ActivityEventEntity::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(page.getContent()).noneMatch(e -> e.getActorId().equals(9L));
        assertThat(page.getContent()).noneMatch(e -> e.getActorId().equals(1L));
    }

    @Test
    void findFeedEvents_afterUnfollow_hidesFormerFolloweeEvents() {
        FollowEntity follow = followRepository.save(FollowEntity.builder()
                .followerId(1L)
                .followingId(2L)
                .build());

        activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(2L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(40L)
                .reviewId(7L)
                .createdAt(LocalDateTime.now())
                .build());

        assertThat(activityEventRepository.findFeedEvents(1L, PageRequest.of(0, 20)).getContent())
                .hasSize(1);

        followRepository.delete(follow);

        assertThat(activityEventRepository.findFeedEvents(1L, PageRequest.of(0, 20)).getContent())
                .isEmpty();
        // activity row can still exist; feed query filters via follow EXISTS
        assertThat(activityEventRepository.count()).isEqualTo(1);
    }
}
