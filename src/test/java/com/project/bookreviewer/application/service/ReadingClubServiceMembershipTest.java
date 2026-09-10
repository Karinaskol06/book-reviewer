package com.project.bookreviewer.application.service;

import com.project.bookreviewer.domain.exception.UnauthorizedException;
import com.project.bookreviewer.domain.model.ClubMembership;
import com.project.bookreviewer.domain.model.ClubMembershipStatus;
import com.project.bookreviewer.domain.model.ClubRole;
import com.project.bookreviewer.domain.model.ReadingClub;
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
class ReadingClubServiceMembershipTest {

    @Mock private ReadingClubRepositoryPort clubRepository;
    @Mock private ClubMembershipRepositoryPort membershipRepository;
    @Mock private ClubPostRepositoryPort postRepository;
    @Mock private PostInsightfulRepositoryPort insightfulRepository;
    @Mock private UserService userService;
    @Mock private BookService bookService;
    @Mock private com.project.bookreviewer.application.mapper.ClubMapper clubMapper;

    @InjectMocks
    private ReadingClubService clubService;

    @Test
    void approveMembership_ownerSetsPendingToActive() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.approveMembership(1L, 10L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubMembershipStatus.ACTIVE);
        assertThat(captor.getValue().getUserId()).isEqualTo(20L);
    }

    @Test
    void rejectMembership_setsDeclined() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.MODERATOR, ClubMembershipStatus.ACTIVE)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.PENDING)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.rejectMembership(1L, 10L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubMembershipStatus.DECLINED);
    }

    @Test
    void joinClub_whenDeclined_resetsToPending() {
        ReadingClub club = ReadingClub.builder().id(1L).isPrivate(true).build();
        when(clubRepository.findById(1L)).thenReturn(Optional.of(club));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.DECLINED)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.joinClub(1L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ClubMembershipStatus.PENDING);
    }

    @Test
    void removeMember_moderatorDeletesActiveMembership() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.MODERATOR, ClubMembershipStatus.ACTIVE)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.ACTIVE)));

        clubService.removeMember(1L, 10L, 20L);

        verify(membershipRepository).deleteByClubIdAndUserId(1L, 20L);
    }

    @Test
    void transferOwnership_promotesTargetAndDemotesFormerOwner() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE)));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                membership(20L, ClubRole.MEMBER, ClubMembershipStatus.ACTIVE)));
        when(membershipRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        clubService.transferOwnership(1L, 10L, 20L);

        ArgumentCaptor<ClubMembership> captor = ArgumentCaptor.forClass(ClubMembership.class);
        verify(membershipRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        List<ClubMembership> saved = captor.getAllValues();
        assertThat(saved).anyMatch(m -> m.getUserId().equals(20L) && m.getRole() == ClubRole.OWNER);
        assertThat(saved).anyMatch(m -> m.getUserId().equals(10L) && m.getRole() == ClubRole.MEMBER);
    }

    @Test
    void deleteClub_cascadesMembershipsPostsAndInsightfuls() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).createdBy(10L).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE)));

        clubService.deleteClub(1L, 10L);

        verify(insightfulRepository).deleteByClubId(1L);
        verify(postRepository).deleteByClubId(1L);
        verify(membershipRepository).deleteByClubId(1L);
        verify(clubRepository).deleteById(1L);
    }

    @Test
    void leaveClub_soleOwnerWithoutTransfer_throws() {
        when(membershipRepository.findByClubIdAndUserId(1L, 10L)).thenReturn(Optional.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE)));
        when(membershipRepository.findByClubId(1L)).thenReturn(List.of(
                membership(10L, ClubRole.OWNER, ClubMembershipStatus.ACTIVE)));

        assertThatThrownBy(() -> clubService.leaveClub(1L, 10L))
                .isInstanceOf(IllegalStateException.class);
        verify(membershipRepository, never()).deleteByClubIdAndUserId(eq(1L), eq(10L));
    }

    private static ClubMembership membership(Long userId, ClubRole role, ClubMembershipStatus status) {
        return ClubMembership.builder()
                .id(userId)
                .clubId(1L)
                .userId(userId)
                .role(role)
                .status(status)
                .build();
    }
}
