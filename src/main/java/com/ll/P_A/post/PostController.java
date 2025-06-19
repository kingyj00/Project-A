package com.ll.P_A.post;

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

    //로그인 확인 공통 메서드
    private Long getLoginUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }

    //전체 게시글 조회 (비회원 가능)
    @GetMapping
    public ResponseEntity<List<PostResponseDto>> getAllPosts() {
        List<PostResponseDto> posts = postService.getAll();
        return ResponseEntity.ok(posts);
    }

    //단일 게시글 조회 (조회수 증가 포함)
    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(@PathVariable Long id) {
        PostResponseDto post = postService.getById(id);
        return ResponseEntity.ok(post);
    }

    // 게시글 작성 (회원만 가능)
    @PostMapping
    public ResponseEntity<Void> createPost(@RequestBody PostRequestDto dto, HttpSession session) {
        Long userId = getLoginUserId(session);
        dto = new PostRequestDto(dto.title(), dto.content(), "user-" + userId); // author 임시 처리
        Long id = postService.create(dto);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }

    // 게시글 수정 (회원만 가능)
    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(@PathVariable Long id, @RequestBody PostRequestDto dto, HttpSession session) {
        getLoginUserId(session); // 로그인 여부만 확인
        postService.update(id, dto);
        return ResponseEntity.ok().build();
    }

    //게시글 삭제 (회원만 가능)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, HttpSession session) {
        getLoginUserId(session);
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }
}