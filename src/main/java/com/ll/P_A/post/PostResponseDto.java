package com.ll.P_A.post;

import java.time.LocalDateTime;

public record PostResponseDto(
        Long id,
        String title,
        String content,
        String author,
        int viewCount,
        int likeCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public PostResponseDto(PostEntity post) {
        this(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getAuthor(),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }
}