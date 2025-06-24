package com.ll.P_A.post;

import com.ll.P_A.security.User;

import java.time.LocalDateTime;

public record PostResponseDto(
        Long id,
        String title,
        String content,
        String authorName,
        Long authorId,
        int viewCount,
        int likeCount,
        boolean likedByMe,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public PostResponseDto(PostEntity post, User loginUser) {
        this(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                extractAuthorName(post.getAuthor()),
                extractAuthorId(post.getAuthor()),
                post.getViewCount(),
                post.getLikeCount(),
                post.isLikedBy(loginUser),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    private static String extractAuthorName(User author) {
        if (author == null) return "(알 수 없음)";
        return author.getUsername();  // 또는 getNickname() 등
    }

    private static Long extractAuthorId(User author) {
        if (author == null) return null;
        return author.getId();
    }
}