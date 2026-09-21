package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreatePostRequest;
import com.project.bookreviewer.application.mapper.ClubPostMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClubDiscussionServiceAccessTest {

    @Mock private ClubPostRepositoryPort postRepository;
    @Mock private PostInsightfulRepositoryPort insightfulRepository;
    @Mock private ClubMembershipRepositoryPort membershipRepository;
    @Mock private ReadingClubRepositoryPort clubRepository;
    @Mock private UserService userService;
    @Mock private ClubPostMapper postMapper;

    @InjectMocks
    private ClubDiscussionService discussionService;

    @Test
    void createPost_pendingMember_isRejected() {
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                ClubMembership.builder()
                        .clubId(1L)
                        .userId(20L)
                        .role(ClubRole.MEMBER)
                        .status(ClubMembershipStatus.PENDING)
                        .build()));

        CreatePostRequest request = new CreatePostRequest();
        request.setContent("hello");

        assertThatThrownBy(() -> discussionService.createPost(1L, 20L, request))
                .isInstanceOf(UnauthorizedException.class);
        verify(postRepository, never()).save(any());
    }

    @Test
    void getClubPosts_privateClub_pendingMember_isRejected() {
        when(clubRepository.findById(1L)).thenReturn(Optional.of(
                ReadingClub.builder().id(1L).isPrivate(true).build()));
        when(membershipRepository.findByClubIdAndUserId(1L, 20L)).thenReturn(Optional.of(
                ClubMembership.builder()
                        .clubId(1L)
                        .userId(20L)
                        .role(ClubRole.MEMBER)
                        .status(ClubMembershipStatus.PENDING)
                        .build()));

        assertThatThrownBy(() -> discussionService.getClubPosts(1L, PageRequest.of(0, 10), 20L))
                .isInstanceOf(UnauthorizedException.class);
    }
}
