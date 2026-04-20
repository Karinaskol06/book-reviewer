package com.project.bookreviewer.application.service;

import com.project.bookreviewer.application.dto.request.CreatePostRequest;
import com.project.bookreviewer.application.dto.response.ClubPostResponse;
import com.project.bookreviewer.application.mapper.ClubPostMapper;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClubDiscussionService {
    private final ClubPostRepositoryPort postRepository;
    private final PostInsightfulRepositoryPort insightfulRepository;
    private final ClubMembershipRepositoryPort membershipRepository;
    private final ReadingClubRepositoryPort clubRepository;
    private final UserService userService;
    private final ClubPostMapper postMapper;

    @Transactional
    public ClubPost createPost(Long clubId, Long authorId, CreatePostRequest request) {
        validateClubMembership(clubId, authorId);

        ClubPost post = ClubPost.builder()
                .clubId(clubId)
                .authorId(authorId)
                .parentPostId(request.getParentPostId())
                .content(request.getContent())
                .insightfulCount(0)
                .build();

        ClubPost saved = postRepository.save(post);
        log.info("Post created: id={}, club={}, author={}", saved.getId(), clubId, authorId);
        return saved;
    }

    @Transactional
    public ClubPost updatePost(Long postId, Long authorId, String content) {
        ClubPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAuthorId().equals(authorId)) {
            // Check if user is moderator/owner of the club
            validateClubRole(post.getClubId(), authorId, ClubRole.OWNER, ClubRole.MODERATOR);
        }

        ClubPost updated = ClubPost.builder()
                .id(post.getId())
                .clubId(post.getClubId())
                .authorId(post.getAuthorId())
                .parentPostId(post.getParentPostId())
                .content(content)
                .insightfulCount(post.getInsightfulCount())
                .createdAt(post.getCreatedAt())
                .build();

        return postRepository.save(updated);
    }

    @Transactional
    public void deletePost(Long postId, Long userId) {
        ClubPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        if (!post.getAuthorId().equals(userId)) {
            validateClubRole(post.getClubId(), userId, ClubRole.OWNER, ClubRole.MODERATOR);
        }

        // Delete replies first
        List<ClubPost> replies = postRepository.findByParentPostId(postId);
        for (ClubPost reply : replies) {
            postRepository.deleteById(reply.getId());
        }

        // Delete insightful records
        insightfulRepository.deleteByPostId(postId);

        postRepository.deleteById(postId);
        log.info("Post deleted: id={}", postId);
    }

    public ClubPostResponse getPost(Long postId, Long userId) {
        ClubPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));
        return enrichPostResponse(post, userId);
    }

    public Page<ClubPostResponse> getClubPosts(Long clubId, Pageable pageable, Long userId) {
        // Verify user can view club (public or member)
        ReadingClub club = clubRepository.findById(clubId)
                .orElseThrow(() -> new ResourceNotFoundException("Club not found"));

        if (club.getIsPrivate() && userId != null) {
            validateClubMembership(clubId, userId);
        }

        Page<ClubPost> posts = postRepository.findByClubIdAndParentPostIdIsNull(clubId, pageable);
        return posts.map(post -> enrichPostResponse(post, userId));
    }

    public Page<ClubPostResponse> getPostReplies(Long postId, Pageable pageable, Long userId) {
        ClubPost parent = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        List<ClubPost> replies = postRepository.findByParentPostId(postId);
        List<ClubPostResponse> responseList = replies.stream()
                .map(reply -> enrichPostResponse(reply, userId))
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), responseList.size());
        return new PageImpl<>(responseList.subList(start, end), pageable, responseList.size());
    }

    @Transactional
    public void toggleInsightful(Long postId, Long userId) {
        ClubPost post = postRepository.findById(postId)
                .orElseThrow(() -> new ResourceNotFoundException("Post not found"));

        validateClubMembership(post.getClubId(), userId);

        if (insightfulRepository.existsByPostIdAndUserId(postId, userId)) {
            insightfulRepository.deleteByPostIdAndUserId(postId, userId);
            decrementInsightfulCount(postId);
            log.info("User {} removed insightful from post {}", userId, postId);
        } else {
            PostInsightful insightful = PostInsightful.builder()
                    .postId(postId)
                    .userId(userId)
                    .build();
            insightfulRepository.save(insightful);
            incrementInsightfulCount(postId);
            log.info("User {} marked post {} as insightful", userId, postId);
        }
    }

    private ClubPostResponse enrichPostResponse(ClubPost post, Long userId) {
        ClubPostResponse response = postMapper.toResponse(post);
        response.setAuthor(userService.buildReviewUserDto(post.getAuthorId()));
        response.setReplyCount(postRepository.countRepliesByPostId(post.getId()));

        if (userId != null) {
            response.setHasInsightful(insightfulRepository.existsByPostIdAndUserId(post.getId(), userId));
        }

        return response;
    }

    private void validateClubMembership(Long clubId, Long userId) {
        if (!membershipRepository.existsByClubIdAndUserId(clubId, userId)) {
            throw new UnauthorizedException("User is not a member of this club");
        }
    }

    private void validateClubRole(Long clubId, Long userId, ClubRole... allowedRoles) {
        ClubMembership membership = membershipRepository.findByClubIdAndUserId(clubId, userId)
                .orElseThrow(() -> new UnauthorizedException("User is not a member of this club"));

        for (ClubRole role : allowedRoles) {
            if (membership.getRole() == role) return;
        }
        throw new UnauthorizedException("User does not have required role");
    }

    private void incrementInsightfulCount(Long postId) {
        ClubPost post = postRepository.findById(postId).orElseThrow();
        ClubPost updated = ClubPost.builder()
                .id(post.getId())
                .clubId(post.getClubId())
                .authorId(post.getAuthorId())
                .parentPostId(post.getParentPostId())
                .content(post.getContent())
                .insightfulCount(post.getInsightfulCount() + 1)
                .createdAt(post.getCreatedAt())
                .build();
        postRepository.save(updated);
    }

    private void decrementInsightfulCount(Long postId) {
        ClubPost post = postRepository.findById(postId).orElseThrow();
        ClubPost updated = ClubPost.builder()
                .id(post.getId())
                .clubId(post.getClubId())
                .authorId(post.getAuthorId())
                .parentPostId(post.getParentPostId())
                .content(post.getContent())
                .insightfulCount(Math.max(0, post.getInsightfulCount() - 1))
                .createdAt(post.getCreatedAt())
                .build();
        postRepository.save(updated);
    }
}