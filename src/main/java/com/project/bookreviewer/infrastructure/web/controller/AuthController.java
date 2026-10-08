package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.LoginRequest;
import com.project.bookreviewer.application.dto.request.RegisterRequest;
import com.project.bookreviewer.application.dto.response.AuthResponse;
import com.project.bookreviewer.application.dto.response.UserProfileResponse;
import com.project.bookreviewer.application.mapper.UserMapper;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.inbound.AuthUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthUseCase authUseCase;
    private final UserMapper userMapper;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = authUseCase.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
        String token = authUseCase.authenticate(request.getUsername(), request.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(userMapper.toAuthResponse(user, token));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        String token = authUseCase.authenticate(request.getUsername(), request.getPassword());
        User user = authUseCase.getCurrentUser();
        return ResponseEntity.ok(userMapper.toAuthResponse(user, token));
    }
}
