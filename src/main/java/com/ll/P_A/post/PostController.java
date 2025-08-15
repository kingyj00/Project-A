package com.ll.P_A.post;

import com.ll.P_A.security.User;
import com.ll.P_A.security.UserService;
import com.ll.P_A.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;

    private Long getLoginUserId(@AuthenticationPrincipal CustomUserDetails loginUser) {
        Long userId = (loginUser != null) ? loginUser.getUser().getId() : null;
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts(
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = (loginUser != null) ? loginUser.getUser().getId() : null;
        User loginUserEntity = (userId != null) ? userService.findById(userId) : null;
        List<PostResponseDto> posts = postService.getAll(loginUserEntity);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = (loginUser != null) ? loginUser.getUser().getId() : null;
        User loginUserEntity = (userId != null) ? userService.findById(userId) : null;
        PostResponseDto post = postService.getById(id, loginUserEntity);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<Void> createPost(
            @Valid @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = getLoginUserId(loginUser);
        User user = userService.findById(userId);

        Long id = postService.create(dto, user);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = getLoginUserId(loginUser);
        // 원본 시그니처 유지: updateByUser(id, dto, userId)
        postService.updateByUser(id, dto, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = getLoginUserId(loginUser);
        // 원본 시그니처 유지: deleteByUser(id, userId)
        postService.deleteByUser(id, userId);
        return ResponseEntity.noContent().build();
    }

    // 좋아요 추가
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = getLoginUserId(loginUser);
        User user = userService.findById(userId);

        // 원본 시그니처 유지: like(id, user)
        postService.like(id, user);
        return ResponseEntity.ok().build();
    }

    // 좋아요 취소
    @PostMapping("/{id}/unlike")
    public ResponseEntity<Void> unlikePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = getLoginUserId(loginUser);
        User user = userService.findById(userId);

        // 원본 시그니처 유지: unlike(id, user)
        postService.unlike(id, user);
        return ResponseEntity.ok().build();
    }

    // 좋아요 누른 여부 확인
    @GetMapping("/{id}/liked")
    public ResponseEntity<Map<String, Boolean>> isLiked(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = (loginUser != null) ? loginUser.getUser().getId() : null;
        if (userId == null) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
        User user = userService.findById(userId);

        // 원본 시그니처 유지: isLikedByUser(id, user)
        boolean liked = postService.isLikedByUser(id, user);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}