package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ObjectStoragePort;
import com.project.bookreviewer.domain.port.outbound.ReviewRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceAvatarTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private UserService userService;

    @Test
    void replaceAvatar_whenOldAvatarExists_deletesOldKeyAndStoresNew() throws Exception {
        User existing = baseUser().avatarUrl("avatars/old.png").build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(objectStoragePort.store(eq("avatars"), eq("new.png"), eq("image/png"), any(InputStream.class), eq(3L)))
                .thenReturn("avatars/new.png");
        when(objectStoragePort.toPublicUrl("avatars/new.png")).thenReturn("/uploads/avatars/new.png");

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1, 2, 3});
        String publicUrl = userService.replaceAvatar(1L, file);

        assertThat(publicUrl).isEqualTo("/uploads/avatars/new.png");
        verify(objectStoragePort).delete("avatars/old.png");
        verify(objectStoragePort).store(eq("avatars"), eq("new.png"), eq("image/png"), any(InputStream.class), eq(3L));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarUrl()).isEqualTo("avatars/new.png");
    }

    @Test
    void replaceAvatar_whenNoOldAvatar_skipsDeleteAndStoresNew() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(baseUser().build()));
        when(objectStoragePort.store(eq("avatars"), eq("new.png"), eq("image/png"), any(InputStream.class), eq(3L)))
                .thenReturn("avatars/new.png");
        when(objectStoragePort.toPublicUrl("avatars/new.png")).thenReturn("/uploads/avatars/new.png");

        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1, 2, 3});
        userService.replaceAvatar(1L, file);

        verify(objectStoragePort, never()).delete(anyString());
        verify(objectStoragePort).store(eq("avatars"), eq("new.png"), eq("image/png"), any(InputStream.class), eq(3L));
    }

    @Test
    void removeAvatar_whenAvatarPresent_deletesAndClears() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(baseUser().avatarUrl("avatars/old.png").build()));

        userService.removeAvatar(1L);

        verify(objectStoragePort).delete("avatars/old.png");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getAvatarUrl()).isNull();
    }

    @Test
    void removeAvatar_whenAvatarNull_isNoOp() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(baseUser().build()));

        userService.removeAvatar(1L);

        verify(objectStoragePort, never()).delete(anyString());
        verify(userRepository, never()).save(any());
    }

    private static User.UserBuilder baseUser() {
        return User.builder()
                .id(1L)
                .username("alice")
                .email("alice@ex.com")
                .password("hash")
                .roles(Set.of(Role.USER))
                .enabled(true);
    }
}
