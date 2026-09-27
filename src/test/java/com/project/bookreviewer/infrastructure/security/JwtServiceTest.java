package com.project.bookreviewer.infrastructure.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);
    }

    @Test
    void generateToken_roundTripsUsername() {
        UserDetails user = User.withUsername("alice").password("n/a").authorities(List.of()).build();

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.validateToken(token, user)).isTrue();
    }

    @Test
    void validateToken_expiredToken_isRejected() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1_000L);
        UserDetails user = User.withUsername("alice").password("n/a").authorities(List.of()).build();

        String expiredToken = jwtService.generateToken(user);

        // JJWT rejects expired tokens during parse (same path validateToken uses).
        assertThatThrownBy(() -> jwtService.validateToken(expiredToken, user))
                .isInstanceOf(ExpiredJwtException.class);
    }
}
