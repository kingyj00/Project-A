package com.ll.P_A.post;

import com.ll.P_A.security.User;
import com.ll.P_A.security.UserService;
import com.ll.P_A.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.Map;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;
    private final UserService userService;

    //로그인 사용자 식별/조회

    /** 로그인 필수: ID 반환 (없으면 401) */
    private Long requireLoginUserId(CustomUserDetails loginUser) {
        Long userId = (loginUser != null) ? loginUser.getUser().getId() : null;
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }

    /** 로그인 필수: 엔티티 반환 (없으면 401) */
    private User requireLoginUser(CustomUserDetails loginUser) {
        return userService.findById(requireLoginUserId(loginUser));
    }

    /** 로그인 선택: 엔티티 or null */
    private User optionalLoginUser(CustomUserDetails loginUser) {
        Long userId = (loginUser != null) ? loginUser.getUser().getId() : null;
        return (userId != null) ? userService.findById(userId) : null;
    }

   //게시글 목록: 페이징/정렬/검색 제공
    @GetMapping
    public ResponseEntity<Page<PostResponseDto>> getAllPosts(
            @AuthenticationPrincipal CustomUserDetails loginUser,
            @RequestParam(name = "keyword", required = false) String keyword,
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        User loginUserEntity = optionalLoginUser(loginUser);
        Page<PostResponseDto> posts = postService.getAll(loginUserEntity, pageable, keyword);
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PostResponseDto> getPost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        User loginUserEntity = optionalLoginUser(loginUser);
        PostResponseDto post = postService.getById(id, loginUserEntity);
        return ResponseEntity.ok(post);
    }

    @PostMapping
    public ResponseEntity<Void> createPost(
            @Valid @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        User user = requireLoginUser(loginUser);
        Long id = postService.create(dto, user);
        return ResponseEntity.created(URI.create("/api/posts/" + id)).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody PostRequestDto dto,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = requireLoginUserId(loginUser);
        postService.updateByUser(id, dto, userId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        Long userId = requireLoginUserId(loginUser);
        postService.deleteByUser(id, userId);
        return ResponseEntity.noContent().build();
    }

    // 좋아요 추가
    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        User user = requireLoginUser(loginUser);
        postService.like(id, user);
        return ResponseEntity.ok().build();
    }

    // 좋아요 취소
    @PostMapping("/{id}/unlike")
    public ResponseEntity<Void> unlikePost(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        User user = requireLoginUser(loginUser);
        postService.unlike(id, user);
        return ResponseEntity.ok().build();
    }

    // 좋아요 누른 여부 확인
    @GetMapping("/{id}/liked")
    public ResponseEntity<Map<String, Boolean>> isLiked(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails loginUser
    ) {
        User user = optionalLoginUser(loginUser);
        boolean liked = (user != null) && postService.isLikedByUser(id, user);
        return ResponseEntity.ok(Map.of("liked", liked));
    }
}