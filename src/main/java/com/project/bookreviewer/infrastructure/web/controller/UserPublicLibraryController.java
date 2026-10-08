package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.response.UserBookStatusResponse;
import com.project.bookreviewer.application.mapper.UserBookStatusMapper;
import com.project.bookreviewer.domain.model.ReadingStatus;
import com.project.bookreviewer.domain.model.UserBookStatus;
import com.project.bookreviewer.domain.port.inbound.UserLibraryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserPublicLibraryController {
    private final UserLibraryUseCase libraryUseCase;
    private final UserBookStatusMapper mapper;

    @GetMapping("/{userId}/library")
    public ResponseEntity<List<UserBookStatusResponse>> getUserLibraryByUserId(
            @PathVariable Long userId,
            @RequestParam(required = false) ReadingStatus status) {
        List<UserBookStatus> library = libraryUseCase.getUserLibrary(userId, status);
        return ResponseEntity.ok(library.stream().map(mapper::toResponse).toList());
    }
}
