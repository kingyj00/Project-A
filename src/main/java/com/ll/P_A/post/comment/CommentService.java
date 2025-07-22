package com.ll.P_A.post.comment;

import com.ll.P_A.global.exception.AuthorizationValidator;
import com.ll.P_A.post.PostEntity;
import com.ll.P_A.post.PostRepository;
import com.ll.P_A.security.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final AuthorizationValidator authValidator;

    // 댓글 작성
    @Transactional
    public Long create(Long postId, CommentRequestDto dto, User user) {
        PostEntity post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시글이 존재하지 않습니다."));

        CommentEntity comment = CommentEntity.builder()
                .content(dto.content())
                .post(post)
                .author(user)
                .build();

        return commentRepository.save(comment).getId();
    }

    // 게시글에 달린 댓글 조회
    @Transactional(readOnly = true)
    public List<CommentResponseDto> getComments(Long postId) {
        return commentRepository.findByPostIdOrderByCreatedAtAsc(postId)
                .stream()
                .map(CommentResponseDto::new)
                .toList();
    }

    // 작성자 권한 검증 포함한 삭제
    @Transactional
    public void deleteByUser(Long commentId, Long userId) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        authValidator.validateAuthor(comment.getAuthor(), userId);
        commentRepository.delete(comment);
    }

    // 작성자 권한 검증 포함한 수정
    @Transactional
    public void updateByUser(Long commentId, Long userId, String newContent) {
        CommentEntity comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new IllegalArgumentException("댓글이 존재하지 않습니다."));

        authValidator.validateAuthor(comment.getAuthor(), userId);
        comment.setContent(newContent);
    }
}