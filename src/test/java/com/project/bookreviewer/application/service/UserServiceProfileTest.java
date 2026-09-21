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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceProfileTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private ReviewRepositoryPort reviewRepository;
    @Mock
    private ObjectStoragePort objectStoragePort;

    @InjectMocks
    private UserService userService;

    @Test
    void updateProfile_savesDisplayNameAboutAndNormalizedLinks() {
        User existing = User.builder()
                .id(1L)
                .username("karina")
                .email("k@ex.com")
                .password("hash")
                .roles(Set.of(Role.USER))
                .enabled(true)
                .createdAt(LocalDateTime.of(2024, 3, 1, 10, 0))
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        userService.updateProfile(
                1L,
                "  Karina Novak  ",
                "  I love books  ",
                List.of("instagram.com/karina", "https://goodreads.com/karina", "instagram.com/karina")
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getDisplayName()).isEqualTo("Karina Novak");
        assertThat(saved.getAboutMe()).isEqualTo("I love books");
        assertThat(saved.getSocialLinks()).containsExactly(
                "https://instagram.com/karina",
                "https://goodreads.com/karina"
        );
        assertThat(saved.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        assertThat(saved.getUsername()).isEqualTo("karina");
    }

    @Test
    void sanitizeSocialLinks_rejectsNonHttpSchemes() {
        assertThatThrownBy(() -> UserService.sanitizeSocialLinks(List.of("javascript:alert(1)")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http");
    }

    @Test
    void sanitizeSocialLinks_capsAtFive() {
        List<String> links = UserService.sanitizeSocialLinks(List.of(
                "https://a.com/1",
                "https://a.com/2",
                "https://a.com/3",
                "https://a.com/4",
                "https://a.com/5",
                "https://a.com/6"
        ));
        assertThat(links).hasSize(5);
    }
}
