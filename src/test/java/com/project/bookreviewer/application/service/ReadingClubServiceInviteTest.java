package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ClubMembershipResponse;
import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.application.mapper.ClubMapper;
import com.project.bookreviewer.domain.exception.ResourceNotFoundException;
import com.project.bookreviewer.domain.exception.UnauthorizedException;
import com.project.bookreviewer.domain.model.ClubMembership;
import com.project.bookreviewer.domain.model.ClubMembershipStatus;
import com.project.bookreviewer.domain.model.ClubRole;
import com.project.bookreviewer.domain.model.ReadingClub;
import com.project.bookreviewer.domain.model.User;
import com.project.bookreviewer.domain.port.outbound.ClubMembershipRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ClubPostRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.PostInsightfulRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReadingClubRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingClubServiceInviteTest {

    @Mock private ReadingClubRepositoryPort clubRepository;
    @Mock private ClubMembershipRepositoryPort membershipRepository;
    @Mock private ClubPostRepositoryPort postRepository;
    @Mock private PostInsightfulRepositoryPort insightfulRepository;
    @Mock private UserService userService;
    @Mock private BookService bookService;
    @Mock private ClubMapper clubMapper;

    @InjectMocks
    private ReadingClubService clubService;

    @Test
    void inviteMember_ownerCreatesPendingMembershipWithInvitedBy() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.empty());
        when(userService.getUserById(20L)).thenReturn(User.builder().id(20L).username("reader").build());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.inviteMember(1L, 10L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        ClubMembership saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(20L);
        assertThat(saved.getStatus()).isEqualTo(ClubMembershipStatus.PENDING);
        assertThat(saved.getRole()).isEqualTo(ClubRole.MEMBER);
        assertThat(saved.getInvitedBy()).isEqualTo(10L);
    }

    @Test
    void inviteMember_reinvitesDeclinedUser() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.DECLINED, null)));
        when(userService.getUserById(20L)).thenReturn(User.builder().id(20L).username("reader").build());
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.inviteMember(1L, 10L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubMembershipStatus.PENDING);
        assertThat(captor.getValue().getInvitedBy()).isEqualTo(10L);
        assertThat(captor.getValue().getId()).isEqualTo(20L);
    }

    @Test
    void inviteMember_publicClub_throws() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(false).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));

        assertThatThrownBy(() -> clubService.inviteMember(1L, 10L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("private");
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void inviteMember_alreadyActive_throws() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.ACTIVE, null)));
        when(userService.getUserById(20L)).thenReturn(User.builder().id(20L).username("reader").build());

        assertThatThrownBy(() -> clubService.inviteMember(1L, 10L, 20L))
                .isInstanceOf(IllegalStateException.class);
        verify(membershipRepository, never()).save(any());
    }

    @Test
    void inviteMember_self_throws() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));

        assertThatThrownBy(() -> clubService.inviteMember(1L, 10L, 10L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void inviteMember_nonModerator_throws() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 15L)).thenReturn(Optional.of(
                membership(15L, ClubRole.MEMBER, ClubMembershipStatus.ACTIVE, null)));

        assertThatThrownBy(() -> clubService.inviteMember(1L, 15L, 20L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void acceptInvite_inviteeBecomesActive() {
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, 10L)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.acceptInvite(1L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubMembershipStatus.ACTIVE);
        assertThat(captor.getValue().getInvitedBy()).isEqualTo(10L);
    }

    @Test
    void acceptInvite_joinRequestWithoutInvitedBy_throws() {
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, null)));

        assertThatThrownBy(() -> clubService.acceptInvite(1L, 20L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("invite");
    }

    @Test
    void declineInvite_setsDeclined() {
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, 10L)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.declineInvite(1L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubMembershipStatus.DECLINED);
    }

    @Test
    void cancelInvite_moderatorDeletesOutgoingInvite() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.MODERATOR, ClubMembershipStatus.ACTIVE, null)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, 10L)));

        clubService.cancelInvite(1L, 10L, 20L);

        verify(membershipRepository).deleteByClubIdAndUserId(1L, 20L);
    }

    @Test
    void cancelInvite_joinRequest_throws() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, null)));

        assertThatThrownBy(() -> clubService.cancelInvite(1L, 10L, 20L))
                .isInstanceOf(IllegalStateException.class);
        verify(membershipRepository, never()).deleteByClubIdAndUserId(eq(1L), eq(20L));
    }

    @Test
    void getMyInvitations_returnsPendingInvitesWithClubDetails() {
        ClubMembership invite = membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, 10L);
        ReadingClub club = ReadingClub.builder().id(1L).name("Private Shelf").isPrivate(true).createdBy(10L).build();
        when(membershipRepository.findByUserIdAndStatus(20L, ClubMembershipStatus.PENDING))
                .thenReturn(List.of(invite));
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(clubMapper.toResponse(club)).thenReturn(ClubResponse.builder().id(1L).name("Private Shelf").build());
        when(membershipRepository.countByClubIdAndStatus(eq(1L), any())).thenReturn(2L);
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(invite));
        when(clubMapper.toMembershipResponse(invite)).thenReturn(ClubMembershipResponse.builder()
                .id(20L).clubId(1L).status("PENDING").invitedBy(10L).build());
        when(userService.buildReviewUserDto(10L)).thenThrow(new ResourceNotFoundException("gone"));

        List<ClubResponse> invitations = clubService.getMyInvitations(20L);

        assertThat(invitations).hasSize(1);
        assertThat(invitations.getFirst().getName()).isEqualTo("Private Shelf");
    }

    @Test
    void getMyInvitations_ignoresJoinRequestsWithoutInvitedBy() {
        ClubMembership joinRequest = membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING, null);
        when(membershipRepository.findByUserIdAndStatus(20L, ClubMembershipStatus.PENDING))
                .thenReturn(List.of(joinRequest));

        List<ClubResponse> invitations = clubService.getMyInvitations(20L);

        assertThat(invitations).isEmpty();
        verify(clubRepository, never()).findById(any());
    }

    @Test
    void inviteMember_unknownUser_propagatesNotFound() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE, null)));
        when(userService.getUserById(99L)).thenThrow(new ResourceNotFoundException("User not found"));

        assertThatThrownBy(() -> clubService.inviteMember(1L, 10L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private static ClubMembership membership(Long userId, ClubRole role, ClubMembershipStatus status, Long invitedBy) {
        return ClubMembership.builder()
                .id(userId)
                .clubId(1L)
                .userId(userId)
                .role(role)
                .status(status)
                .invitedBy(invitedBy)
                .build();
    }
}
