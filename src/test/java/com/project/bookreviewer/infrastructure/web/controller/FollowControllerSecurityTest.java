package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.service.FollowService;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = FollowController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = JwtAuthenticationFilter.class
        )
)
@Import({WebMvcSecurityTestConfig.class, GlobalExceptionHandler.class})
class FollowControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FollowService followService;
    @MockBean
    private SecurityUtils securityUtils;

    @Test
    @WithMockUser(username = "alice")
    void follow_authenticated_returnsOk() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(post("/api/users/2/follow"))
                .andExpect(status().isOk());

        verify(followService).follow(1L, 2L);
    }

    @Test
    void follow_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/users/2/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void unfollow_authenticated_returnsNoContent() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(1L);

        mockMvc.perform(delete("/api/users/2/follow"))
                .andExpect(status().isNoContent());

        verify(followService).unfollow(1L, 2L);
    }

    @Test
    void unfollow_anonymous_returnsUnauthorized() throws Exception {
        mockMvc.perform(delete("/api/users/2/follow"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "alice")
    void follow_self_returnsBadRequest() throws Exception {
        when(securityUtils.getCurrentUserId()).thenReturn(2L);
        doThrow(new IllegalArgumentException("Cannot follow yourself"))
                .when(followService).follow(2L, 2L);

        mockMvc.perform(post("/api/users/2/follow"))
                .andExpect(status().isBadRequest());
    }
}
