package com.ll.P_A.post;

import com.ll.P_A.security.User;
import com.ll.P_A.security.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long create(PostRequestDto dto, User author) {
        PostEntity post = PostEntity.builder()
                .title(dto.title())
                .content(dto.content())
                .author(author)
                .build();
        return postRepository.save(post).getId();
    }

    // 로그인 유저 기반 전체 목록 조회 (likedByMe 판단 포함)
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAll(User loginUser) {
        return postRepository.findAll().stream()
                .map(post -> new PostResponseDto(post, loginUser))
                .toList();
    }

    // 로그인 유저 기반 단일 조회 (likedByMe 판단 포함)
    @Transactional(readOnly = true)
    public PostResponseDto getById(Long id, User loginUser) {
        PostEntity post = getEntityById(id);
        post.increaseViewCount();
        return new PostResponseDto(post, loginUser);
    }

    @Transactional
    public void update(Long id, PostRequestDto dto) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        post.update(dto.title(), dto.content());
    }

    @Transactional
    public void delete(Long id) {
        if (!postRepository.existsById(id)) {
            throw new IllegalArgumentException("이미 삭제되었거나 없는 글입니다.");
        }
        postRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public PostEntity getEntityById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    // 좋아요 추가
    @Transactional
    public void like(Long postId, User user) {
        PostEntity post = getEntityById(postId);
        post.like(user);
    }

    // 좋아요 취소
    @Transactional
    public void unlike(Long postId, User user) {
        PostEntity post = getEntityById(postId);
        post.unlike(user);
    }

    // 좋아요 누른 여부 확인
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, User user) {
        PostEntity post = getEntityById(postId);
        return post.isLikedBy(user);
    }
}