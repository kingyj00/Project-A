package com.ll.P_A.post;

import com.ll.P_A.security.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Lob
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User author;

    private int viewCount;
    private int likeCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "post_likes",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id")
    )
    private Set<User> likedUsers = new HashSet<>(); // Builder 사용 시도 null 방지

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.viewCount = 0;
        this.likeCount = 0;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public void like(User user) {
        if (!likedUsers.contains(user)) {
            likedUsers.add(user);
            this.likeCount++;
        }
    }

    public void unlike(User user) {
        if (likedUsers.contains(user)) {
            likedUsers.remove(user);
            this.likeCount = Math.max(0, this.likeCount - 1);
        }
    }

    public boolean isLikedBy(User user) {
        return likedUsers.contains(user);
    }
}