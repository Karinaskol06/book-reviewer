package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.ReviewResponse;
import com.project.bookreviewer.application.mapper.ReviewMapper;
import com.project.bookreviewer.application.service.BookService;
import com.project.bookreviewer.application.service.ReviewService;
import com.project.bookreviewer.domain.model.Review;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ReviewHelpfulRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import com.project.bookreviewer.infrastructure.security.JwtAuthenticationFilter;
import com.project.bookreviewer.infrastructure.security.ReviewAuthorization;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import com.project.bookreviewer.infrastructure.security.WebMvcSecurityTestConfig;
import com.project.bookreviewer.infrastructure.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ReviewController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import({
        WebMvcSecurityTestConfig.class,
        ReviewService.class,
        ReviewAuthorization.class,
        GlobalExceptionHandler.class
})
class ReviewControllerSecurityTest {

    private static final String REVIEW_JSON = """
            {
              "rating": 4,
              "verdict": "Solid read",
              "whoIsItFor": "Fans of quiet stories",
              "whoIsItNotFor": "Readers wanting action",
              "pacing": "MEDIUM",
              "mood": ["HOPEFUL"],
              "tags": ["HOPEFUL"]
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewRepositoryPort reviewRepository;
    @MockBean
    private ReviewHelpfulRepositoryPort reviewHelpfulRepository;
    @MockBean
    private BookService bookService;
    @MockBean
    private ApplicationEventPublisher applicationEventPublisher;
    @MockBean
    private ReviewMapper reviewMapper;
    @MockBean
    private SecurityUtils securityUtils;
    @MockBean
    private UserRepositoryPort userRepository;

    @Test
    @WithMockUser(username = "alice")
    void updateReview_owner_returnsOk() throws Exception {
        stubOwnership("alice", 1L);
        Review existing = ownedReview();
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));
        when(securityUtils.getCurrentUserIdOrNull()).thenReturn(1L);
        when(reviewHelpfulRepository.existsByReviewIdAndUserId(10L, 1L)).thenReturn(false);
        when(reviewMapper.toResponse(any(Review.class), eq(true))).thenReturn(
                ReviewResponse.builder().id(10L).verdict("Solid read").build());

        mockMvc.perform(put("/api/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_JSON))
                .andExpect(status().isOk());

        verify(reviewRepository).save(any(Review.class));
        verify(bookService).updateBookRatingStats(5L);
    }

    @Test
    @WithMockUser(username = "bob")
    void updateReview_nonOwner_returnsForbidden() throws Exception {
        stubOwnership("bob", 2L);

        mockMvc.perform(put("/api/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_JSON))
                .andExpect(status().isForbidden());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void updateReview_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/reviews/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REVIEW_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void deleteReview_owner_returnsNoContent() throws Exception {
        stubOwnership("alice", 1L);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(ownedReview()));

        mockMvc.perform(delete("/api/reviews/10"))
                .andExpect(status().isNoContent());

        verify(reviewRepository).deleteById(10L);
        verify(bookService).updateBookRatingStats(5L);
    }

    @Test
    @WithMockUser(username = "bob")
    void deleteReview_nonOwner_returnsForbidden() throws Exception {
        stubOwnership("bob", 2L);

        mockMvc.perform(delete("/api/reviews/10"))
                .andExpect(status().isForbidden());

        verify(reviewRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteReview_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/reviews/10"))
                .andExpect(status().isUnauthorized());
    }

    private void stubOwnership(String username, Long userId) {
        when(reviewRepository.findUserIdByReviewId(10L)).thenReturn(Optional.of(1L));
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(
                User.builder()
                        .id(userId)
                        .username(username)
                        .email(username + "@ex.com")
                        .password("x")
                        .roles(Set.of(Role.USER))
                        .enabled(true)
                        .build()
        ));
    }

    private static Review ownedReview() {
        return Review.builder()
                .id(10L)
                .userId(1L)
                .bookId(5L)
                .rating(3)
                .verdict("Old")
                .helpfulCount(0)
                .createdAt(LocalDateTime.of(2024, 1, 10, 12, 0))
                .build();
    }
}
