package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.exception.AuthenticationException;
import com.project.bookreviewer.domain.exception.UserAlreadyExistsException;
import com.project.bookreviewer.domain.model.Role;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.UserRepositoryPort;
import com.project.bookreviewer.infrastructure.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepositoryPort userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void register_hashesPassword_assignsUserRole_andPersists() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@ex.com")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("hashed-secret");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User incoming = inv.getArgument(0);
            return User.builder()
                    .id(1L)
                    .username(incoming.getUsername())
                    .email(incoming.getEmail())
                    .password(incoming.getPassword())
                    .roles(incoming.getRoles())
                    .enabled(incoming.isEnabled())
                    .build();
        });

        User saved = authService.register("alice", "alice@ex.com", "secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User persisted = captor.getValue();
        assertThat(persisted.getPassword()).isEqualTo("hashed-secret");
        assertThat(persisted.getRoles()).isEqualTo(Set.of(Role.USER));
        assertThat(persisted.isEnabled()).isTrue();
        assertThat(persisted.getUsername()).isEqualTo("alice");
        assertThat(persisted.getEmail()).isEqualTo("alice@ex.com");
        assertThat(saved.getId()).isEqualTo(1L);
        verify(passwordEncoder).encode("secret");
    }

    @Test
    void register_duplicateUsername_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("alice", "alice@ex.com", "secret"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Username");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@ex.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register("alice", "alice@ex.com", "secret"))
                .isInstanceOf(UserAlreadyExistsException.class)
                .hasMessageContaining("Email");

        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void authenticate_success_returnsJwtAndSetsSecurityContext() {
        Authentication authentication = mock(Authentication.class);
        UserDetails principal = mock(UserDetails.class);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn("jwt-token");

        String token = authService.authenticate("alice", "secret");

        assertThat(token).isEqualTo("jwt-token");
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(jwtService).generateToken(principal);
    }

    @Test
    void authenticate_invalidCredentials_throwsDomainAuthenticationException() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> authService.authenticate("alice", "wrong"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Invalid username or password");

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtService, never()).generateToken(any());
    }
}
