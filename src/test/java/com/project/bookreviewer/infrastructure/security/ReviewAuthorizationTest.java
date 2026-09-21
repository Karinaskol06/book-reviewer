package com.project.bookreviewer.infrastructure.security;

import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewAuthorizationTest {

    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private UserRepositoryPort userRepository;

    @InjectMocks
    private ReviewAuthorization reviewAuthorization;

    @Test
    void isReviewOwner_trueWhenUsernameOwnsReview() {
        when(reviewRepository.findUserIdByReviewId(10L)).thenReturn(Optional.of(1L));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(
                User.builder().id(1L).username("alice").build()
        ));

        assertThat(reviewAuthorization.isReviewOwner(10L, "alice")).isTrue();
    }

    @Test
    void isReviewOwner_falseWhenDifferentUser() {
        when(reviewRepository.findUserIdByReviewId(10L)).thenReturn(Optional.of(1L));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(
                User.builder().id(2L).username("bob").build()
        ));

        assertThat(reviewAuthorization.isReviewOwner(10L, "bob")).isFalse();
    }

    @Test
    void isReviewOwner_falseWhenReviewMissing() {
        when(reviewRepository.findUserIdByReviewId(404L)).thenReturn(Optional.empty());

        assertThat(reviewAuthorization.isReviewOwner(404L, "alice")).isFalse();
    }
}
