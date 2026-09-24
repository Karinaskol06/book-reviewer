package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.TasteProfileResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.application.mapper.ReviewMapper;
import com.project.bookreviewer.application.mapper.UserMapper;
import com.project.bookreviewer.application.service.ReviewService;
import com.project.bookreviewer.application.service.TasteProfileService;
import com.project.bookreviewer.application.service.UserBookStatusService;
import com.project.bookreviewer.application.service.UserService;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.infrastructure.security.JwtAuthenticationFilter;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import com.project.bookreviewer.infrastructure.security.WebMvcSecurityTestConfig;
import com.project.bookreviewer.infrastructure.web.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = UserProfileController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import({WebMvcSecurityTestConfig.class, GlobalExceptionHandler.class})
class UserProfileControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;
    @MockBean
    private UserBookStatusService userBookStatusService;
    @MockBean
    private ReviewService reviewService;
    @MockBean
    private TasteProfileService tasteProfileService;
    @MockBean
    private SecurityUtils securityUtils;
    @MockBean
    private ReviewMapper reviewMapper;
    @MockBean
    private UserMapper userMapper;

    @Test
    @WithMockUser(username = "alice")
    void updateProfile_authenticated_returnsNoContent() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "displayName": "Alice",
                                  "aboutMe": "I love books",
                                  "socialLinks": ["https://instagram.com/alice"]
                                }
                                """))
                .andExpect(status().isNoContent());

        verify(userService).updateProfile(
                eq(1L),
                eq("Alice"),
                eq("I love books"),
                eq(List.of("https://instagram.com/alice"))
        );
    }

    @Test
    void updateProfile_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/users/me/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void getUserProfile_authenticated_returnsOk() throws Exception {
        User user = User.builder()
                .id(5L)
                .username("natalia")
                .email("n@ex.com")
                .password("x")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();
        when(userService.getUserById(5L)).thenReturn(user);
        when(userMapper.toProfileResponse(user)).thenReturn(
                UserProfileResponse.builder().id(5L).username("natalia").build());
        when(userBookStatusService.getUserLibrary(eq(5L), any())).thenReturn(List.of());
        when(reviewService.countReviewsByUser(5L)).thenReturn(2);

        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isOk());
    }

    @Test
    void getUserProfile_anonymous_returnsUnauthorized() throws Exception {
        // /api/users/** is authenticated under current SecurityConfig (not permitAll)
        mockMvc.perform(get("/api/users/5"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void getTasteProfile_authenticated_returnsOk() throws Exception {
        when(userService.getUserById(5L)).thenReturn(
                User.builder().id(5L).username("natalia").email("n@ex.com").password("x")
                        .roles(Set.of(Role.USER)).enabled(true).build());
        when(tasteProfileService.getTasteProfile(5L)).thenReturn(
                TasteProfileResponse.builder().sampleSize(3).build());

        mockMvc.perform(get("/api/users/5/taste-profile"))
                .andExpect(status().isOk());
    }

    @Test
    void getTasteProfile_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/5/taste-profile"))
                .andExpect(status().isUnauthorized());
    }
}
