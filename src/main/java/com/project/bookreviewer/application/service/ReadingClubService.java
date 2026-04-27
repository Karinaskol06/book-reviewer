package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreateClubRequest;
import com.project.bookreviewer.application.dto.request.UpdateClubRequest;
import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.mapper.ClubMapper;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.exception.UnauthorizedException;
import com.project.bookreviewer.domain.model.*;
import com.project.bookreviewer.domain.port.outbound.ClubMembershipRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReadingClubRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReadingClubService {
    private final ReadingClubRepositoryPort clubRepository;
    private final ClubMembershipRepositoryPort membershipRepository;
    private final UserService userService;
    private final BookService bookService;
    private final ClubMapper clubMapper;

    @Transactional
    public ReadingClub createClub(Long userId, CreateClubRequest request) {
        ReadingClub club = ReadingClub.builder()
                .name(request.getName())
                .description(request.getDescription())
                .focus(request.getFocus())
                .currentBookId(request.getCurrentBookId())
                .isPrivate(request.getIsPrivate())
                .coverImageUrl(request.getCoverImageUrl())
                .nextMeetingAt(request.getNextMeetingAt())
                .createdBy(userId)
                .build();
        ReadingClub saved = clubRepository.save(club);

        ClubMembership membership = ClubMembership.builder()
                .clubId(saved.getId())
                .userId(userId)
                .role(ClubRole.OWNER)
                .status(ClubMembershipStatus.ACTIVE)
                .build();
        membershipRepository.save(membership);

        log.info("Club created: id={}, name={}, owner={}", saved.getId(), saved.getName(), userId);
        return saved;
    }

    @Transactional
    public ReadingClub updateClub(Long clubId, Long userId, UpdateClubRequest request) {
        ReadingClub club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        validateClubRole(clubId, userId, ClubRole.OWNER, ClubRole.MODERATOR);

        ReadingClub updated = ReadingClub.builder()
                .id(club.getId())
                .name(request.getName() != null ? request.getName() : club.getName())
                .description(request.getDescription() != null ? request.getDescription() : club.getDescription())
                .focus(request.getFocus() != null ? request.getFocus() : club.getFocus())
                .currentBookId(request.getCurrentBookId() != null ? request.getCurrentBookId() : club.getCurrentBookId())
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : club.getIsPrivate())
                .coverImageUrl(request.getCoverImageUrl() != null ? request.getCoverImageUrl() : club.getCoverImageUrl())
                .nextMeetingAt(request.getNextMeetingAt() != null ? request.getNextMeetingAt() : club.getNextMeetingAt())
                .createdBy(club.getCreatedBy())
                .createdAt(club.getCreatedAt())
                .build();

        return clubRepository.save(updated);
    }

    @Transactional
    public void deleteClub(Long clubId, Long userId) {
        ReadingClub club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        validateClubRole(clubId, userId, ClubRole.OWNER);

        clubRepository.deleteById(clubId);
        log.info("Club deleted: id={}", clubId);
    }

    public ReadingClub getClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));
    }

    @Transactional(readOnly = true)
    public ClubResponse getClubDetails(Long clubId, Long userId) {
        ReadingClub club = getClub(clubId);
        ClubResponse response = clubMapper.toResponse(club);

        // Safely fetch current book
        if (club.getCurrentBookId() != null) {
            try {
                response.setCurrentBook(bookService.getBookSummary(club.getCurrentBookId()));
            } catch (ResourceNotFoundException e) {
                log.warn("Current book {} not found for club {}", club.getCurrentBookId(), clubId);
                response.setCurrentBook(null);
            }
        }

        // Membership stats
        response.setMemberCount(membershipRepository.countByClubId(clubId));
        response.setPendingCount(membershipRepository.countByClubIdAndStatus(clubId, ClubMembershipStatus.PENDING));

        // User membership (if authenticated)
        if (userId != null) {
            membershipRepository.findByClubIdAndUserId(clubId, userId)
                    .ifPresent(m -> response.setUserMembership(clubMapper.toMembershipResponse(m)));
        }

        // Owner info
        try {
            response.setOwner(userService.buildReviewUserDto(club.getCreatedBy()));
        } catch (ResourceNotFoundException e) {
            log.warn("Owner {} not found for club {}", club.getCreatedBy(), clubId);
            response.setOwner(null);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ClubResponse> getPublicClubs(Pageable pageable, Long userId) {
        Page<ReadingClub> clubs = clubRepository.findAllPublic(pageable);
        return clubs.map(club -> getClubDetails(club.getId(), userId));
    }

    public Page<ClubResponse> getUserClubs(Long userId, Pageable pageable) {
        List<ReadingClub> clubs = clubRepository.findByMemberUserId(userId);
        List<ClubResponse> responses = clubs.stream()
                .map(club -> getClubDetails(club.getId(), userId))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responses.size());
        return new PageImpl<>(responses.subList(start, end), pageable, responses.size());
    }

    @Transactional
    public void joinClub(Long clubId, Long userId) {
        ReadingClub club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        if (membershipRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw new IllegalStateException("Already a member or pending");
        }

        ClubMembership membership = ClubMembership.builder()
                .clubId(clubId)
                .userId(userId)
                .role(ClubRole.MEMBER)
                .status(club.getIsPrivate() ? ClubMembershipStatus.PENDING : ClubMembershipStatus.ACTIVE)
                .build();
        membershipRepository.save(membership);
        log.info("User {} joined club {} with status {}", userId, clubId, membership.getStatus());
    }

    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        ClubMembership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        if (membership.getRole() == ClubRole.OWNER) {
            // Transfer ownership or prevent leaving
            long otherOwners = membershipRepository.findByClubId(clubId).stream()
                    .filter(m -> m.getRole() == ClubRole.OWNER && !m.getUserId().equals(userId))
                    .count();
            if (otherOwners == 0) {
                throw new IllegalStateException("Owner cannot leave without transferring ownership");
            }
        }

        membershipRepository.deleteByClubIdAndUserId(clubId, userId);
        log.info("User {} left club {}", userId, clubId);
    }

    @Transactional
    public void promoteToModerator(Long clubId, Long promoterId, Long userId) {
        validateClubRole(clubId, promoterId, ClubRole.OWNER);
        updateMemberRole(clubId, userId, ClubRole.MODERATOR);
    }

    @Transactional
    public void demoteToMember(Long clubId, Long demoterId, Long userId) {
        validateClubRole(clubId, demoterId, ClubRole.OWNER);
        updateMemberRole(clubId, userId, ClubRole.MEMBER);
    }

    @Transactional
    public void setCurrentBook(Long clubId, Long userId, Long bookId) {
        validateClubRole(clubId, userId, ClubRole.OWNER, ClubRole.MODERATOR);
        ReadingClub club = getClub(clubId);
        ReadingClub updated = ReadingClub.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .focus(club.getFocus())
                .currentBookId(bookId)
                .isPrivate(club.getIsPrivate())
                .coverImageUrl(club.getCoverImageUrl())
                .nextMeetingAt(club.getNextMeetingAt())
                .createdBy(club.getCreatedBy())
                .createdAt(club.getCreatedAt())
                .build();
        clubRepository.save(updated);
        log.info("Club {} current book set to {}", clubId, bookId);
    }

    @Transactional
    public void setNextMeeting(Long clubId, Long userId, LocalDateTime meetingTime) {
        validateClubRole(clubId, userId, ClubRole.OWNER, ClubRole.MODERATOR);
        ReadingClub club = getClub(clubId);
        ReadingClub updated = ReadingClub.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .focus(club.getFocus())
                .currentBookId(club.getCurrentBookId())
                .isPrivate(club.getIsPrivate())
                .coverImageUrl(club.getCoverImageUrl())
                .nextMeetingAt(meetingTime)
                .createdBy(club.getCreatedBy())
                .createdAt(club.getCreatedAt())
                .build();
        clubRepository.save(updated);
        log.info("Club {} next meeting set to {}", clubId, meetingTime);
    }

    public List<ClubMembershipResponse> getClubMembers(Long clubId) {
        return membershipRepository.findByClubId(clubId).stream()
                .map(m -> {
                    var response = clubMapper.toMembershipResponse(m);
                    response.setUser(userService.buildReviewUserDto(m.getUserId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void validateClubRole(Long clubId, Long userId, ClubRole... allowedRoles) {
        ClubMembership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this club"));

        for (ClubRole role : allowedRoles) {
            if (membership.getRole() == role) return;
        }
        throw new UnauthorizedException("User does not have required role");
    }

    private void updateMemberRole(Long clubId, Long userId, ClubRole newRole) {
        ClubMembership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        ClubMembership updated = ClubMembership.builder()
                .id(membership.getId())
                .clubId(membership.getClubId())
                .userId(membership.getUserId())
                .role(newRole)
                .status(membership.getStatus())
                .joinedAt(membership.getJoinedAt())
                .build();
        membershipRepository.save(updated);
    }

}