package com.project.bookreviewer.domain.port.inbound;

import com.project.bookreviewer.application.dto.request.CreateClubRequest;
import com.project.bookreviewer.application.dto.request.UpdateClubRequest;
import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.domain.model.ReadingClub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Driving port for reading-club lifecycle and membership.
 */
public interface ReadingClubUseCase {
    ReadingClub createClub(Long userId, CreateClubRequest request);

    ReadingClub updateClub(Long clubId, Long userId, UpdateClubRequest request);

    void deleteClub(Long clubId, Long userId);

    ClubResponse getClubDetails(Long clubId, Long userId);

    Page<ClubResponse> getPublicClubs(Pageable pageable, Long userId);

    Page<ClubResponse> getUserClubs(Long userId, Pageable pageable);

    void joinClub(Long clubId, Long userId);

    void leaveClub(Long clubId, Long userId);

    void approveMembership(Long clubId, Long actorId, Long targetUserId);

    void rejectMembership(Long clubId, Long actorId, Long targetUserId);

    void removeMember(Long clubId, Long actorId, Long targetUserId);

    void transferOwnership(Long clubId, Long ownerId, Long newOwnerId);

    void promoteToModerator(Long clubId, Long promoterId, Long userId);

    void demoteToMember(Long clubId, Long demoterId, Long userId);

    void setCurrentBook(Long clubId, Long userId, Long bookId);

    void setNextMeeting(Long clubId, Long userId, LocalDateTime meetingTime, String meetingLink);

    List<ClubMembershipResponse> getClubMembers(Long clubId, Long viewerId);

    List<ClubMembershipResponse> getPendingMembers(Long clubId, Long viewerId);
}
