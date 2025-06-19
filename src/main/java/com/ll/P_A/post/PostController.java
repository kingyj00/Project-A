package com.ll.P_A.post;

import com.ll.P_A.user.User;
import com.ll.P_A.user.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

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
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        List<PostResponseDto> posts = postService.getAll();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        PostResponseDto post = postService.getById(id);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<Void> createPost(@RequestBody PostRequestDto dto, HttpSession session) {
        Long userId = getLoginUserId(session);
        User user = userService.findById(userId);

        Long id = postService.create(dto, user);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id, @RequestBody PostRequestDto dto, HttpSession session) {
        Long userId = getLoginUserId(session);
        PostEntity post = postService.getEntityById(id);

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 수정할 수 있습니다.");
        }

        postService.update(id, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, HttpSession session) {
        Long userId = getLoginUserId(session);
        PostEntity post = postService.getEntityById(id);

        if (!post.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "작성자만 삭제할 수 있습니다.");
        }

        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}