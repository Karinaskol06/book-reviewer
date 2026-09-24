package com.project.bookreviewer.infrastructure.security;

import com.project.bookreviewer.infrastructure.storage.StorageProperties;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Lightweight security for {@code @WebMvcTest}: mirrors production auth rules without JWT filter,
 * so {@code @WithMockUser} can drive authentication and {@code @PreAuthorize} still runs.
 * Provides {@link StorageProperties} so production {@code WebMvcConfig} can load in the slice.
 */
@TestConfiguration
@EnableMethodSecurity
public class WebMvcSecurityTestConfig {

    @Bean
    StorageProperties storageProperties() {
        return new StorageProperties();
    }

    @Bean
    SecurityFilterChain webMvcSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/books/covers").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/books").authenticated()
                        .requestMatchers(HttpMethod.PUT, "/api/books/*").authenticated()
                        .requestMatchers("/api/home/**", "/api/books/**", "/api/genres").permitAll()
                        .requestMatchers("/uploads-book-reviewer/**").permitAll()
                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
