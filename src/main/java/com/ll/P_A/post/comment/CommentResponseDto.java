package com.ll.P_A.post.comment;

import java.time.LocalDateTime;

public record CommentResponseDto(
        Long id,
        String content,
        String authorName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public CommentResponseDto(CommentEntity comment) {
        this(
                comment.getId(),
                comment.getContent(),
                comment.getAuthor().getUsername(),
                comment.getCreatedAt(),
                comment.getUpdatedAt()
        );
    }
}