package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.response.ClubResponse;
import com.project.bookreviewer.application.mapper.ClubMapper;
import com.project.bookreviewer.domain.model.ReadingClub;
import com.project.bookreviewer.domain.port.outbound.ClubMembershipRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ClubPostRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.PostInsightfulRepositoryPort;
import com.project.bookreviewer.domain.port.outbound.ReadingClubRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingClubServiceSearchTest {

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
    void searchPublicClubs_delegatesToRepositoryWithQuery() {
        Pageable pageable = PageRequest.of(0, 12);
        ReadingClub club = ReadingClub.builder().id(5L).name("Sci-Fi Circle").isPrivate(false).createdBy(1L).build();
        when(clubRepository.searchPublic("sci", pageable)).thenReturn(new PageImpl<>(List.of(club)));
        when(clubRepository.findById(5L)).thenReturn(Optional.of(club));
        when(clubMapper.toResponse(club)).thenReturn(ClubResponse.builder().id(5L).name("Sci-Fi Circle").build());
        when(membershipRepository.countByClubIdAndStatus(eq(5L), any())).thenReturn(0L);
        when(userService.buildReviewUserDto(1L)).thenThrow(new com.project.bookreviewer.domain.exception.ResourceNotFoundException("gone"));

        Page<ClubResponse> result = clubService.searchPublicClubs("sci", pageable, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getName()).isEqualTo("Sci-Fi Circle");
        verify(clubRepository).searchPublic("sci", pageable);
    }

    @Test
    void searchPublicClubs_blankQuery_fallsBackToPublicListing() {
        Pageable pageable = PageRequest.of(0, 12);
        when(clubRepository.findAllPublic(pageable)).thenReturn(Page.empty(pageable));

        Page<ClubResponse> result = clubService.searchPublicClubs("   ", pageable, 1L);

        assertThat(result.getContent()).isEmpty();
        verify(clubRepository).findAllPublic(pageable);
    }

    @Test
    void searchUserClubs_filtersActiveMembershipsByQuery() {
        Pageable pageable = PageRequest.of(0, 12);
        ReadingClub match = ReadingClub.builder().id(1L).name("Poetry Night").isPrivate(true).createdBy(10L).build();
        ReadingClub other = ReadingClub.builder().id(2L).name("History Buffs").isPrivate(false).createdBy(10L).build();
        when(clubRepository.findByMemberUserId(10L)).thenReturn(List.of(match, other));
        when(clubRepository.findById(1L)).thenReturn(Optional.of(match));
        when(clubMapper.toResponse(match)).thenReturn(ClubResponse.builder().id(1L).name("Poetry Night").build());
        when(membershipRepository.countByClubIdAndStatus(eq(1L), any())).thenReturn(3L);
        when(userService.buildReviewUserDto(10L)).thenThrow(new com.project.bookreviewer.domain.exception.ResourceNotFoundException("gone"));

        Page<ClubResponse> result = clubService.searchUserClubs(10L, "poetry", pageable);

        assertThat(result.getContent()).extracting(ClubResponse::getId).containsExactly(1L);
    }
}
