package com.ll.P_A.post;

import com.ll.P_A.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;

    // author: String → User
    @Transactional
    public Long create(PostRequestDto dto, User author) {
        PostEntity post = PostEntity.builder()
                .title(dto.title())
                .content(dto.content())
                .author(author)
                .build();
        return postRepository.save(post).getId();
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> getAll() {
        return postRepository.findAll().stream()
                .map(PostResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public PostResponseDto getById(Long id) {
        PostEntity post = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
        post.increaseViewCount();
        return new PostResponseDto(post);
    }

    // 수정/삭제 시 본인 확인은 Controller에서 처리 중
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

    // 엔티티 직접 반환 (작성자 ID 확인용)
    @Transactional(readOnly = true)
    public PostEntity getEntityById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 게시글입니다."));
    }
}