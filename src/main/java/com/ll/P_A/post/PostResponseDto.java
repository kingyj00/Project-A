package com.ll.P_A.post;

import com.ll.P_A.security.User;

import java.time.LocalDateTime;

public record PostResponseDto(
        Long id,
        String title,
        String content,
        String authorName,     //변경: author → authorName
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
                extractAuthorName(post.getAuthor()),
                post.getViewCount(),
                post.getLikeCount(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private static String extractAuthorName(User author) {
        if (author == null) return "(알 수 없음)";
        return author.getUsername();  // 또는 getNickname(), getEmail() 등 원하는 필드
    }
}