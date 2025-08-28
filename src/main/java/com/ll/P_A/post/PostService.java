package com.ll.P_A.post;

import com.ll.P_A.global.exception.AuthorizationValidator;
import com.ll.P_A.security.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final AuthorizationValidator authValidator; // 권한 검증기 주입

    @Transactional
    public Long create(PostRequestDto dto, User author) {
        PostEntity post = PostEntity.builder()
                .title(dto.title())
                .content(dto.content())
                .author(author)
                .build();
        return postRepository.save(post).getId();
    }

    /** 로그인 유저 기반 전체 목록 조회 (likedByMe 판단 포함) */
    @Transactional(readOnly = true)
    public List<PostResponseDto> getAll(User loginUser) {
        return postRepository.findAll().stream()
                .map(post -> new PostResponseDto(post, loginUser))
                .toList();
    }

    /** 로그인 유저 기반 단일 조회 (조회수 증가 + likedByMe 판단 포함) */
    @Transactional // 조회수 증가가 있어 readOnly=false
    public PostResponseDto getById(Long id, User loginUser) {
        PostEntity post = getEntityById(id);
        post.increaseViewCount();
        return new PostResponseDto(post, loginUser);
    }

    /** 작성자 권한 포함한 수정 로직 */
    @Transactional
    public void updateByUser(Long id, PostRequestDto dto, Long userId) {
        PostEntity post = getEntityById(id);
        authValidator.validateAuthor(post.getAuthor(), userId); // 권한 검증
        post.update(dto.title(), dto.content());
    }

    /** 작성자 권한 포함한 삭제 로직 */
    @Transactional
    public void deleteByUser(Long id, Long userId) {
        PostEntity post = getEntityById(id);
        authValidator.validateAuthor(post.getAuthor(), userId); // 권한 검증
        postRepository.delete(post);
    }

    /** 내부용: 게시글 엔티티 조회 */
    @Transactional(readOnly = true)
    public PostEntity getEntityById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }

    /** 좋아요 추가 */
    @Transactional
    public void like(Long postId, User user) {
        PostEntity post = getEntityById(postId);
        if (post.getAuthor().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인 게시글에는 좋아요를 누를 수 없습니다.");
        }
        post.like(user);
    }

    /** 좋아요 취소 */
    @Transactional
    public void unlike(Long postId, User user) {
        PostEntity post = getEntityById(postId);
        post.unlike(user);
    }

    /** 좋아요 눌렀는지 여부 확인 */
    @Transactional(readOnly = true)
    public boolean isLikedByUser(Long postId, User user) {
        PostEntity post = getEntityById(postId);
        return post.isLikedBy(user);
    }
}