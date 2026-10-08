package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.UserBookStatusRequest;
import com.project.bookreviewer.application.dto.response.UserBookStatusResponse;
import com.project.bookreviewer.application.mapper.UserBookStatusMapper;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.inbound.UserLibraryUseCase;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserLibraryController {
    private final UserLibraryUseCase libraryUseCase;
    private final UserBookStatusMapper mapper;
    private final SecurityUtils securityUtils;

    @PostMapping("/books/{bookId}/status")
    public ResponseEntity<UserBookStatusResponse> setStatus(
            @PathVariable Long bookId,
            @Valid @RequestBody UserBookStatusRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        UserBookStatus status = libraryUseCase.setStatus(userId, bookId, request.getStatus());
        return ResponseEntity.ok(mapper.toResponse(status));
    }

    @GetMapping("/books/{bookId}/status")
    public ResponseEntity<UserBookStatusResponse> getStatus(@PathVariable Long bookId) {
        Long userId = securityUtils.getCurrentUserId();
        return libraryUseCase.getStatus(userId, bookId)
                .map(mapper::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @DeleteMapping("/books/{bookId}/status")
    public ResponseEntity<Void> clearStatus(@PathVariable Long bookId) {
        Long userId = securityUtils.getCurrentUserId();
        libraryUseCase.clearStatus(userId, bookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/library")
    public ResponseEntity<List<UserBookStatusResponse>> getLibrary(
            @RequestParam(required = false) ReadingStatus status) {
        Long userId = securityUtils.getCurrentUserId();
        List<UserBookStatus> library = libraryUseCase.getUserLibrary(userId, status);
        return ResponseEntity.ok(library.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList()));
    }

}
