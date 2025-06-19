package com.ll.P_A.post;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // 전체 게시글 조회 (비회원도 가능)
    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        List<PostResponseDto> posts = postService.getAll();
        return ResponseEntity.ok(posts);
    }

    // 단일 게시글 조회 (조회수 증가 포함)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        PostResponseDto post = postService.getById(id);
        return ResponseEntity.ok(post);
    }

    // 게시글 작성 (회원 전제, 현재는 인증 체크 생략)
    @PostMapping
    public ResponseEntity<Void> createPost(@RequestBody PostRequestDto dto) {
        Long id = postService.create(dto);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();  // HTTP 201
    }

    // 게시글 수정
    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id, @RequestBody PostRequestDto dto) {
        postService.update(id, dto);
        return ResponseEntity.ok().build();
    }

    // 게시글 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();  // HTTP 204
    }

    @PostMapping
    public ResponseEntity<Void> createPost(@RequestBody PostRequestDto dto, HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build(); // 401 Unauthorized
        }

        dto = new PostRequestDto(dto.title(), dto.content(), "user-" + userId); // 예시용 author 세팅
        Long id = postService.create(dto);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }
}