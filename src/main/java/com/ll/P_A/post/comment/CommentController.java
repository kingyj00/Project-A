package com.ll.P_A.post.comment;

import com.ll.P_A.security.User;
import com.ll.P_A.security.UserService;
import com.ll.P_A.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    /* ---------- 헬퍼: 로그인 사용자 식별/조회 ---------- */

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

    /* ------------------- API ------------------- */

    @PostMapping
    public ResponseEntity<Void> create(@PathVariable Long postId,
                                       @Valid @RequestBody CommentRequestDto dto,
                                       @AuthenticationPrincipal CustomUserDetails loginUser) {
        User user = requireLoginUser(loginUser);
        Long id = commentService.create(postId, dto, user);
        return ResponseEntity.created(URI.create("/api/posts/" + postId + "/comments/" + id)).build();
    }

    @GetMapping
    public ResponseEntity<List<CommentResponseDto>> getAll(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getComments(postId));
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId,
                                       @PathVariable Long commentId,
                                       @AuthenticationPrincipal CustomUserDetails loginUser) {
        Long userId = requireLoginUserId(loginUser);
        commentService.deleteByUser(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(@PathVariable Long postId,
                                       @PathVariable Long commentId,
                                       @Valid @RequestBody CommentRequestDto dto,
                                       @AuthenticationPrincipal CustomUserDetails loginUser) {
        Long userId = requireLoginUserId(loginUser);
        // 원본 시그니처 유지: updateByUser(commentId, userId, dto.content())
        commentService.updateByUser(commentId, userId, dto.content());
        return ResponseEntity.noContent().build();
    }
}