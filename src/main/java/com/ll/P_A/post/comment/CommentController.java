package com.ll.P_A.post.comment;

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
@RequestMapping("/api/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    private Long getUserId(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return userId;
    }

    @PostMapping
    public ResponseEntity<Void> create(@PathVariable Long postId,
                                       @Valid @RequestBody CommentRequestDto dto,
                                       HttpSession session) {
        Long userId = getUserId(session);
        User user = userService.findById(userId);
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
                                       HttpSession session) {
        Long userId = getUserId(session);
        commentService.deleteByUser(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> update(@PathVariable Long postId,
                                       @PathVariable Long commentId,
                                       @Valid @RequestBody CommentRequestDto dto,
                                       HttpSession session) {
        Long userId = getUserId(session);
        commentService.updateByUser(commentId, userId, dto.content());
        return ResponseEntity.noContent().build();
    }
}