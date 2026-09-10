package com.project.bookreviewer.infrastructure.web.controller;

import com.project.bookreviewer.application.dto.request.CreateClubRequest;
import com.project.bookreviewer.application.dto.request.UpdateClubRequest;
import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.dto.response.ClubResponse;
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
import java.util.Map;

@RestController
@RequestMapping("/api/clubs")
@RequiredArgsConstructor
public class ReadingClubController {
    private final ReadingClubService clubService;
    private final SecurityUtils securityUtils;

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

    @GetMapping
    public ResponseEntity<Page<ClubResponse>> getPublicClubs(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(clubService.getPublicClubs(pageable, userId));
    }

    @GetMapping("/my")
    public ResponseEntity<Page<ClubResponse>> getMyClubs(
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(clubService.getUserClubs(userId, pageable));
    }

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
        Long userId = securityUtils.getCurrentUserIdOrNull();
        return ResponseEntity.ok(clubService.getClubMembers(clubId, userId));
    }

    @GetMapping("/{clubId}/members/pending")
    public ResponseEntity<List<ClubMembershipResponse>> getPendingMembers(@PathVariable Long clubId) {
        Long userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(clubService.getPendingMembers(clubId, userId));
    }

    @PutMapping("/{clubId}/members/{userId}/approve")
    public ResponseEntity<Void> approveMember(@PathVariable Long clubId, @PathVariable Long userId) {
        clubService.approveMembership(clubId, securityUtils.getCurrentUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/members/{userId}/reject")
    public ResponseEntity<Void> rejectMember(@PathVariable Long clubId, @PathVariable Long userId) {
        clubService.rejectMembership(clubId, securityUtils.getCurrentUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{clubId}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable Long clubId, @PathVariable Long userId) {
        clubService.removeMember(clubId, securityUtils.getCurrentUserId(), userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{clubId}/members/{userId}/transfer")
    public ResponseEntity<Void> transferOwnership(@PathVariable Long clubId, @PathVariable Long userId) {
        clubService.transferOwnership(clubId, securityUtils.getCurrentUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/members/{userId}/promote")
    public ResponseEntity<Void> promoteToModerator(@PathVariable Long clubId, @PathVariable Long userId) {
        clubService.promoteToModerator(clubId, securityUtils.getCurrentUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/members/{userId}/demote")
    public ResponseEntity<Void> demoteToMember(@PathVariable Long clubId, @PathVariable Long userId) {
        clubService.demoteToMember(clubId, securityUtils.getCurrentUserId(), userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/current-book")
    public ResponseEntity<Void> setCurrentBook(
            @PathVariable Long clubId,
            @RequestParam Long bookId) {
        clubService.setCurrentBook(clubId, securityUtils.getCurrentUserId(), bookId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{clubId}/next-meeting")
    public ResponseEntity<Void> setNextMeeting(
            @PathVariable Long clubId,
            @RequestBody Map<String, Object> body) {
        LocalDateTime meetingTime = body.get("meetingTime") != null
                ? LocalDateTime.parse(body.get("meetingTime").toString())
                : null;
        String meetingLink = body.get("meetingLink") != null ? body.get("meetingLink").toString() : null;
        clubService.setNextMeeting(clubId, securityUtils.getCurrentUserId(), meetingTime, meetingLink);
        return ResponseEntity.ok().build();
    }
}
