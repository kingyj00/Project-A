package com.ll.P_A.post.comment;

import java.time.LocalDateTime;

public record CommentResponseDto(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt
) {
    public CommentResponseDto(Comment comment) {
        this(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getUsername(),
                comment.getCreatedAt()
        );
    }
}