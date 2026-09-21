package com.project.bookreviewer.infrastructure.persistence.repository;

import com.project.bookreviewer.domain.model.ActivityType;
import com.project.bookreviewer.infrastructure.persistence.entity.ActivityEventEntity;
import com.project.bookreviewer.infrastructure.persistence.entity.FollowEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:feedevents;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class ActivityFeedQueryTest {

    @Autowired
    private JpaActivityEventRepository activityEventRepository;
    @Autowired
    private JpaFollowRepository followRepository;

    @Test
    void findFeedEvents_includesReviewedEventsFromFollowedUsers() {
        // user 1 follows user 2
        followRepository.save(FollowEntity.builder()
                .followerId(1L)
                .followingId(2L)
                .build());

        activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(2L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(40L)
                .reviewId(7L)
                .createdAt(LocalDateTime.now().minusHours(1))
                .build());

        activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(2L)
                .targetUserId(1L)
                .type(ActivityType.STARTED_READING)
                .bookId(41L)
                .createdAt(LocalDateTime.now().minusHours(2))
                .build());

        // noise: event for someone user 1 does not follow
        activityEventRepository.save(ActivityEventEntity.builder()
                .actorId(9L)
                .targetUserId(1L)
                .type(ActivityType.REVIEWED)
                .bookId(50L)
                .reviewId(8L)
                .createdAt(LocalDateTime.now())
                .build());

        Page<ActivityEventEntity> page = activityEventRepository.findFeedEvents(1L, PageRequest.of(0, 20));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).anyMatch(e ->
                e.getType() == ActivityType.REVIEWED && e.getReviewId().equals(7L));
        assertThat(page.getContent()).noneMatch(e -> e.getActorId().equals(9L));
    }
}
