package com.ll.P_A;

import com.ll.P_A.post.PostEntity;
import com.ll.P_A.post.PostRepository;
import com.ll.P_A.post.comment.*;
import com.ll.P_A.security.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CommentServiceTest {

    private CommentRepository commentRepository;
    private PostRepository postRepository;
    private CommentService commentService;

    @BeforeEach
    void setUp() {
        commentRepository = mock(CommentRepository.class);
        postRepository = mock(PostRepository.class);
        commentService = new CommentService(commentRepository, postRepository);
    }

    @Test
    void create_shouldSaveComment() {
        // given
        User user = User.builder().id(1L).username("user").build();
        PostEntity post = PostEntity.builder().id(1L).title("post").build();
        CommentRequestDto dto = new CommentRequestDto("test comment");

        Comment comment = Comment.builder()
                .id(100L)
                .content("test comment")
                .author(user)
                .post(post)
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // when
        Long commentId = commentService.create(1L, dto, user);

        // then
        assertThat(commentId).isEqualTo(100L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void getComments_shouldReturnCommentResponseDtoList() {
        // given
        Comment comment = Comment.builder()
                .id(1L)
                .content("hello")
                .build();

        when(commentRepository.findByPostIdOrderByCreatedAtAsc(1L))
                .thenReturn(List.of(comment));

        // when
        List<CommentResponseDto> result = commentService.getComments(1L);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).toString()).contains("hello"); // indirect validation
    }

    @Test
    void deleteByUser_shouldRemoveCommentIfUserMatches() {
        // given
        User user = User.builder().id(1L).username("u").build();
        Comment comment = Comment.builder()
                .id(1L)
                .content("bye")
                .author(user)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        // when
        commentService.deleteByUser(1L, 1L);

        // then
        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteByUser_shouldThrowIfUserMismatch() {
        // given
        User author = User.builder().id(1L).build();
        Comment comment = Comment.builder()
                .id(1L)
                .content("secret")
                .author(author)
                .build();

        when(commentRepository.findById(1L)).thenReturn(Optional.of(comment));

        // when & then
        assertThatThrownBy(() -> commentService.deleteByUser(1L, 2L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("작성자만 삭제할 수 있습니다.");
    }
}