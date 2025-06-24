package com.ll.P_A.post;

import com.ll.P_A.security.User;
import com.ll.P_A.security.UserService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private Long getLoginUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User loginUser = (userId != null) ? userService.findById(userId) : null;
        List<PostResponseDto> posts = postService.getAll(loginUser);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User loginUser = (userId != null) ? userService.findById(userId) : null;
        PostResponseDto post = postService.getById(id, loginUser);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<Void> createPost(@Valid @RequestBody PostRequestDto dto, HttpSession session) {
        Long userId = getLoginUserId(session);
        User user = userService.findById(userId);

        Long id = postService.create(dto, user);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequestDto dto, HttpSession session) {
        Long userId = getLoginUserId(session);
        postService.updateByUser(id, dto, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, HttpSession session) {
        Long userId = getLoginUserId(session);
        postService.deleteByUser(id, userId);
        return ResponseEntity.noContent().build();
    }

    // 좋아요 추가
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likePost(@PathVariable Long id, HttpSession session) {
        Long userId = getLoginUserId(session);
        User user = userService.findById(userId);

        postService.like(id, user);
        return ResponseEntity.ok().build();
    }

    // 좋아요 취소
    @PostMapping("/{id}/unlike")
    public ResponseEntity<Void> unlikePost(@PathVariable Long id, HttpSession session) {
        Long userId = getLoginUserId(session);
        User user = userService.findById(userId);

        postService.unlike(id, user);
        return ResponseEntity.ok().build();
    }

    // 좋아요 누른 여부 확인
    @GetMapping("/{id}/liked")
    public ResponseEntity<Map<String, Boolean>> isLiked(@PathVariable Long id, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");

        if (userId == null) {
            return ResponseEntity.ok(Map.of("liked", false));
        }

        User user = userService.findById(userId);
        boolean liked = postService.isLikedByUser(id, user);

        return ResponseEntity.ok(Map.of("liked", liked));
    }
}