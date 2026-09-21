package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreateClubRequest;
import com.project.bookreviewer.application.dto.request.UpdateClubRequest;
import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.application.mapper.ClubMapper;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.exception.UnauthorizedException;
import com.project.bookreviewer.domain.model.*;
import com.project.bookreviewer.domain.port.outbound.ClubMembershipRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ClubPostRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.PostInsightfulRepositoryPort;
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
    private final ClubPostRepositoryPort postRepository;
    private final PostInsightfulRepositoryPort insightfulRepository;
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
                .isPrivate(Boolean.TRUE.equals(request.getIsPrivate()))
                .coverImageUrl(request.getCoverImageUrl())
                .nextMeetingAt(request.getNextMeetingAt())
                .meetingLink(request.getMeetingLink())
                .createdBy(userId)
                .build();
        ReadingClub saved = clubRepository.save(club);

        membershipRepository.save(ClubMembership.builder()
                .clubId(saved.getId())
                .userId(userId)
                .role(ClubRole.OWNER)
                .status(ClubMembershipStatus.ACTIVE)
                .build());

        log.info("Club created: id={}, name={}, owner={}", saved.getId(), saved.getName(), userId);
        return saved;
    }

    @Transactional
    public ReadingClub updateClub(Long clubId, Long userId, UpdateClubRequest request) {
        ReadingClub club = getClub(clubId);
        validateActiveRole(clubId, userId, ClubRole.OWNER, ClubRole.MODERATOR);

        return clubRepository.save(copyClub(club)
                .name(request.getName() != null ? request.getName() : club.getName())
                .description(request.getDescription() != null ? request.getDescription() : club.getDescription())
                .focus(request.getFocus() != null ? request.getFocus() : club.getFocus())
                .currentBookId(request.getCurrentBookId() != null ? request.getCurrentBookId() : club.getCurrentBookId())
                .isPrivate(request.getIsPrivate() != null ? request.getIsPrivate() : club.getIsPrivate())
                .coverImageUrl(request.getCoverImageUrl() != null ? request.getCoverImageUrl() : club.getCoverImageUrl())
                .nextMeetingAt(request.getNextMeetingAt() != null ? request.getNextMeetingAt() : club.getNextMeetingAt())
                .meetingLink(request.getMeetingLink() != null ? request.getMeetingLink() : club.getMeetingLink())
                .build());
    }

    @Transactional
    public void deleteClub(Long clubId, Long userId) {
        getClub(clubId);
        validateActiveRole(clubId, userId, ClubRole.OWNER);

        insightfulRepository.deleteByClubId(clubId);
        postRepository.deleteByClubId(clubId);
        membershipRepository.deleteByClubId(clubId);
        clubRepository.deleteById(clubId);
        log.info("Club deleted with cascade: id={}", clubId);
    }

    public ReadingClub getClub(Long clubId) {
        return clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));
    }

    @Transactional(readOnly = true)
    public ClubResponse getClubDetails(Long clubId, Long userId) {
        ReadingClub club = getClub(clubId);
        ClubResponse response = clubMapper.toResponse(club);

        if (club.getCurrentBookId() != null) {
            try {
                response.setCurrentBook(bookService.getBookSummary(club.getCurrentBookId()));
            } catch (ResourceNotFoundException e) {
                response.setCurrentBook(null);
            }
        }

        response.setMemberCount(membershipRepository.countByClubIdAndStatus(clubId, ClubMembershipStatus.ACTIVE));
        boolean canSeePending = userId != null && isOwnerOrModerator(clubId, userId);
        response.setPendingCount(canSeePending
                ? membershipRepository.countByClubIdAndStatus(clubId, ClubMembershipStatus.PENDING)
                : 0L);

        if (userId != null) {
            membershipRepository.findByClubIdAndUserId(clubId, userId)
                    .ifPresent(m -> response.setUserMembership(clubMapper.toMembershipResponse(m)));
        }

        try {
            response.setOwner(userService.buildReviewUserDto(club.getCreatedBy()));
        } catch (ResourceNotFoundException e) {
            response.setOwner(null);
        }

        return response;
    }

    @Transactional(readOnly = true)
    public Page<ClubResponse> getPublicClubs(Pageable pageable, Long userId) {
        return clubRepository.findAllPublic(pageable).map(club -> getClubDetails(club.getId(), userId));
    }

    public Page<ClubResponse> getUserClubs(Long userId, Pageable pageable) {
        List<ClubResponse> responses = clubRepository.findByMemberUserId(userId).stream()
                .map(club -> getClubDetails(club.getId(), userId))
                .collect(Collectors.toList());
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        List<ClubResponse> pageContent = start >= responses.size() ? List.of() : responses.subList(start, end);
        return new PageImpl<>(pageContent, pageable, responses.size());
    }

    @Transactional
    public void joinClub(Long clubId, Long userId) {
        ReadingClub club = getClub(clubId);
        var existing = membershipRepository.findByClubIdAndUserId(clubId, userId);

        if (existing.isPresent()) {
            ClubMembership membership = existing.get();
            if (membership.getStatus() == ClubMembershipStatus.DECLINED) {
                membershipRepository.save(ClubMembership.builder()
                        .id(membership.getId())
                        .clubId(membership.getClubId())
                        .userId(membership.getUserId())
                        .role(ClubRole.MEMBER)
                        .status(ClubMembershipStatus.PENDING)
                        .joinedAt(membership.getJoinedAt())
                        .build());
                return;
            }
            throw new IllegalStateException("Already a member or pending");
        }

        ClubMembershipStatus status = Boolean.TRUE.equals(club.getIsPrivate())
                ? ClubMembershipStatus.PENDING
                : ClubMembershipStatus.ACTIVE;
        membershipRepository.save(ClubMembership.builder()
                .clubId(clubId)
                .userId(userId)
                .role(ClubRole.MEMBER)
                .status(status)
                .build());
    }

    @Transactional
    public void leaveClub(Long clubId, Long userId) {
        ClubMembership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));

        if (membership.getRole() == ClubRole.OWNER) {
            long otherOwners = membershipRepository.findByClubId(clubId).stream()
                    .filter(m -> m.getRole() == ClubRole.OWNER && !m.getUserId().equals(userId))
                    .count();
            if (otherOwners == 0) {
                throw new IllegalStateException("Owner cannot leave without transferring ownership");
            }
        }

        membershipRepository.deleteByClubIdAndUserId(clubId, userId);
    }

    @Transactional
    public void approveMembership(Long clubId, Long actorId, Long targetUserId) {
        validateActiveRole(clubId, actorId, ClubRole.OWNER, ClubRole.MODERATOR);
        ClubMembership pending = requireMembership(clubId, targetUserId);
        if (pending.getStatus() != ClubMembershipStatus.PENDING) {
            throw new IllegalStateException("Membership is not pending");
        }
        membershipRepository.save(withStatus(pending, ClubMembershipStatus.ACTIVE));
    }

    @Transactional
    public void rejectMembership(Long clubId, Long actorId, Long targetUserId) {
        validateActiveRole(clubId, actorId, ClubRole.OWNER, ClubRole.MODERATOR);
        ClubMembership pending = requireMembership(clubId, targetUserId);
        if (pending.getStatus() != ClubMembershipStatus.PENDING) {
            throw new IllegalStateException("Membership is not pending");
        }
        membershipRepository.save(withStatus(pending, ClubMembershipStatus.DECLINED));
    }

    @Transactional
    public void removeMember(Long clubId, Long actorId, Long targetUserId) {
        validateActiveRole(clubId, actorId, ClubRole.OWNER, ClubRole.MODERATOR);
        ClubMembership target = requireMembership(clubId, targetUserId);
        if (target.getRole() == ClubRole.OWNER) {
            throw new IllegalStateException("Cannot remove the club owner");
        }
        if (target.getStatus() != ClubMembershipStatus.ACTIVE) {
            throw new IllegalStateException("Can only remove active members");
        }
        membershipRepository.deleteByClubIdAndUserId(clubId, targetUserId);
    }

    @Transactional
    public void transferOwnership(Long clubId, Long ownerId, Long newOwnerId) {
        validateActiveRole(clubId, ownerId, ClubRole.OWNER);
        ClubMembership target = requireMembership(clubId, newOwnerId);
        if (target.getStatus() != ClubMembershipStatus.ACTIVE) {
            throw new IllegalStateException("New owner must be an active member");
        }
        ClubMembership owner = requireMembership(clubId, ownerId);

        membershipRepository.save(withRole(target, ClubRole.OWNER));
        membershipRepository.save(withRole(owner, ClubRole.MEMBER));
    }

    @Transactional
    public void promoteToModerator(Long clubId, Long promoterId, Long userId) {
        validateActiveRole(clubId, promoterId, ClubRole.OWNER);
        ClubMembership target = requireMembership(clubId, userId);
        if (target.getStatus() != ClubMembershipStatus.ACTIVE) {
            throw new IllegalStateException("Only active members can be promoted");
        }
        membershipRepository.save(withRole(target, ClubRole.MODERATOR));
    }

    @Transactional
    public void demoteToMember(Long clubId, Long demoterId, Long userId) {
        validateActiveRole(clubId, demoterId, ClubRole.OWNER);
        ClubMembership target = requireMembership(clubId, userId);
        if (target.getRole() == ClubRole.OWNER) {
            throw new IllegalStateException("Cannot demote the owner");
        }
        membershipRepository.save(withRole(target, ClubRole.MEMBER));
    }

    @Transactional
    public void setCurrentBook(Long clubId, Long userId, Long bookId) {
        validateActiveRole(clubId, userId, ClubRole.OWNER, ClubRole.MODERATOR);
        ReadingClub club = getClub(clubId);
        clubRepository.save(copyClub(club).currentBookId(bookId).build());
    }

    @Transactional
    public void setNextMeeting(Long clubId, Long userId, LocalDateTime meetingTime, String meetingLink) {
        validateActiveRole(clubId, userId, ClubRole.OWNER, ClubRole.MODERATOR);
        ReadingClub club = getClub(clubId);
        clubRepository.save(copyClub(club)
                .nextMeetingAt(meetingTime)
                .meetingLink(meetingLink != null ? meetingLink : club.getMeetingLink())
                .build());
    }

    @Transactional(readOnly = true)
    public List<ClubMembershipResponse> getClubMembers(Long clubId, Long viewerId) {
        return membershipRepository.findByClubIdAndStatus(clubId, ClubMembershipStatus.ACTIVE).stream()
                .map(m -> {
                    var response = clubMapper.toMembershipResponse(m);
                    response.setUser(userService.buildReviewUserDto(m.getUserId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ClubMembershipResponse> getPendingMembers(Long clubId, Long viewerId) {
        validateActiveRole(clubId, viewerId, ClubRole.OWNER, ClubRole.MODERATOR);
        return membershipRepository.findByClubIdAndStatus(clubId, ClubMembershipStatus.PENDING).stream()
                .map(m -> {
                    var response = clubMapper.toMembershipResponse(m);
                    response.setUser(userService.buildReviewUserDto(m.getUserId()));
                    return response;
                })
                .collect(Collectors.toList());
    }

    private void validateActiveRole(Long clubId, Long userId, ClubRole... allowedRoles) {
        ClubMembership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this club"));
        if (membership.getStatus() != ClubMembershipStatus.ACTIVE) {
            throw new UnauthorizedException("User is not an active member of this club");
        }
        for (ClubRole role : allowedRoles) {
            if (membership.getRole() == role) {
                return;
            }
        }
        throw new UnauthorizedException("User does not have required role");
    }

    private boolean isOwnerOrModerator(Long clubId, Long userId) {
        return membershipRepository.findByClubIdAndUserId(clubId, userId)
                .filter(m -> m.getStatus() == ClubMembershipStatus.ACTIVE)
                .filter(m -> m.getRole() == ClubRole.OWNER || m.getRole() == ClubRole.MODERATOR)
                .isPresent();
    }

    private ClubMembership requireMembership(Long clubId, Long userId) {
        return membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Membership not found"));
    }

    private static ClubMembership withStatus(ClubMembership membership, ClubMembershipStatus status) {
        return ClubMembership.builder()
                .id(membership.getId())
                .clubId(membership.getClubId())
                .userId(membership.getUserId())
                .role(membership.getRole())
                .status(status)
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    private static ClubMembership withRole(ClubMembership membership, ClubRole role) {
        return ClubMembership.builder()
                .id(membership.getId())
                .clubId(membership.getClubId())
                .userId(membership.getUserId())
                .role(role)
                .status(membership.getStatus())
                .joinedAt(membership.getJoinedAt())
                .build();
    }

    private static ReadingClub.ReadingClubBuilder copyClub(ReadingClub club) {
        return ReadingClub.builder()
                .id(club.getId())
                .name(club.getName())
                .description(club.getDescription())
                .focus(club.getFocus())
                .currentBookId(club.getCurrentBookId())
                .isPrivate(club.getIsPrivate())
                .coverImageUrl(club.getCoverImageUrl())
                .nextMeetingAt(club.getNextMeetingAt())
                .meetingLink(club.getMeetingLink())
                .createdBy(club.getCreatedBy())
                .createdAt(club.getCreatedAt());
    }
}
