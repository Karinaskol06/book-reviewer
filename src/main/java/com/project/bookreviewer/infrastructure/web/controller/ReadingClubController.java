package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.CreateClubRequest;
import com.project.bookreviewer.application.dto.request.UpdateClubRequest;
import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.application.mapper.ClubMapper;
import com.project.bookreviewer.application.service.ReadingClubService;
import com.project.bookreviewer.domain.model.ReadingClub;
import com.project.bookreviewer.infrastructure.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ReadingClubController {
    private final ReadingClubService clubService;
    private final SecurityUtils securityUtils;
    private final ClubMapper clubMapper;

    // Club CRUD

    @PostMapping
    public ResponseEntity<ClubResponse> createClub(@Valid @RequestBody CreateClubRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ReadingClub club = clubService.createClub(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(clubService.getClubDetails(club.getId(), userId));
    }

    @GetMapping("/{clubId}")
    public ResponseEntity<ClubResponse> getClub(@PathVariable Long clubId) {
        Long userId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(clubService.getClubDetails(clubId, userId));
    }

    @PutMapping("/{clubId}")
    public ResponseEntity<ClubResponse> updateClub(
            @PathVariable Long clubId,
            @Valid @RequestBody UpdateClubRequest request) {
        Long userId = securityUtils.getCurrentUserId();
        ReadingClub club = clubService.updateClub(clubId, userId, request);
        return ResponseEntity.ok(clubService.getClubDetails(club.getId(), userId));
    }

    @DeleteMapping("/{clubId}")
    public ResponseEntity<Void> deleteClub(@PathVariable Long clubId) {
        Long userId = securityUtils.getCurrentUserId();
        clubService.deleteClub(clubId, userId);
        return ResponseEntity.noContent().build();
    }

    // Club Discovery

    @GetMapping
    public ResponseEntity<Page<ClubResponse>> getPublicClubs(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(clubService.getPublicClubs(pageable, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ClubResponse>> getMyClubs(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(clubService.getUserClubs(userId, pageable));
    }

    // Membership Management

    @PostMapping("/{clubId}/join")
    public ResponseEntity<Void> joinClub(@PathVariable Long clubId) {
        Long userId = securityUtils.getCurrentUserId();
        clubService.joinClub(clubId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clubId}/leave")
    public ResponseEntity<Void> leaveClub(@PathVariable Long clubId) {
        Long userId = securityUtils.getCurrentUserId();
        clubService.leaveClub(clubId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{clubId}/members")
    public ResponseEntity<List<ClubMembershipResponse>> getClubMembers(@PathVariable Long clubId) {
        return ResponseEntity.ok(clubService.getClubMembers(clubId));
    }

    @PutMapping("/{clubId}/members/{userId}/approve")
    public ResponseEntity<Void> approveMember(
            @PathVariable Long clubId,
            @PathVariable Long userId) {
        Long approverId = securityUtils.getCurrentUserId();
        clubService.approveMember(clubId, approverId, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clubId}/members/{userId}/reject")
    public ResponseEntity<Void> rejectMember(
            @PathVariable Long clubId,
            @PathVariable Long userId) {
        Long rejectorId = securityUtils.getCurrentUserId();
        clubService.rejectMember(clubId, rejectorId, userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{clubId}/members/{userId}/promote")
    public ResponseEntity<Void> promoteToModerator(
            @PathVariable Long clubId,
            @PathVariable Long userId) {
        Long promoterId = securityUtils.getCurrentUserId();
        clubService.promoteToModerator(clubId, promoterId, userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/members/{userId}/demote")
    public ResponseEntity<Void> demoteToMember(
            @PathVariable Long clubId,
            @PathVariable Long userId) {
        Long demoterId = securityUtils.getCurrentUserId();
        clubService.demoteToMember(clubId, demoterId, userId);
        return ResponseEntity.ok().build();
    }

    // Club Settings

    @PutMapping("/{clubId}/current-book")
    public ResponseEntity<Void> setCurrentBook(
            @PathVariable Long clubId,
            @RequestParam Long bookId) {
        Long userId = securityUtils.getCurrentUserId();
        clubService.setCurrentBook(clubId, userId, bookId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/next-meeting")
    public ResponseEntity<Void> setNextMeeting(
            @PathVariable Long clubId,
            @RequestParam LocalDateTime meetingTime) {
        Long userId = securityUtils.getCurrentUserId();
        clubService.setNextMeeting(clubId, userId, meetingTime);
        return ResponseEntity.ok().build();
    }
}