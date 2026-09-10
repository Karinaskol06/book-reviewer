package com.project.bookreviewer.application.mapper;

import com.project.bookreviewer.application.dto.response.AuthResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserMapperAvatarTest {

    @Mock
    private ObjectStoragePort objectStoragePort;

    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        userMapper = new UserMapperImpl();
        userMapper.objectStoragePort = objectStoragePort;
    }

    @Test
    void toProfileResponse_convertsStorageKeyToPublicUrl() {
        User user = User.builder()
                .id(2L)
                .username("karina")
                .email("k@ex.com")
                .password("x")
                .avatarUrl("avatars/72c333b8-dbc1-4952-8dc3-ed09f62d9f65.jpg")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        when(objectStoragePort.toPublicUrl("avatars/72c333b8-dbc1-4952-8dc3-ed09f62d9f65.jpg"))
                .thenReturn("/uploads-book-reviewer/avatars/72c333b8-dbc1-4952-8dc3-ed09f62d9f65.jpg");

        UserProfileResponse response = userMapper.toProfileResponse(user);

        assertThat(response.getAvatarUrl())
                .isEqualTo("/uploads-book-reviewer/avatars/72c333b8-dbc1-4952-8dc3-ed09f62d9f65.jpg");
    }

    @Test
    void toAuthResponse_convertsStorageKeyToPublicUrl() {
        User user = User.builder()
                .id(2L)
                .username("karina")
                .email("k@ex.com")
                .password("x")
                .avatarUrl("avatars/abc.jpg")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .build();

        when(objectStoragePort.toPublicUrl("avatars/abc.jpg"))
                .thenReturn("/uploads-book-reviewer/avatars/abc.jpg");

        AuthResponse response = userMapper.toAuthResponse(user, "token");

        assertThat(response.getAvatarUrl()).isEqualTo("/uploads-book-reviewer/avatars/abc.jpg");
    }
}
