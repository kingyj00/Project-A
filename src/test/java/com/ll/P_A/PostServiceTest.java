package com.ll.P_A;

import com.ll.P_A.post.PostEntity;
import com.ll.P_A.post.PostRepository;
import com.ll.P_A.post.PostRequestDto;
import com.ll.P_A.post.PostService;
import com.ll.P_A.security.User;
import com.ll.P_A.security.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PostServiceTest {

    private PostRepository postRepository;
    private UserRepository userRepository;
    private PostService postService;

    @BeforeEach
    void setUp() {
        postRepository = mock(PostRepository.class);
        userRepository = mock(UserRepository.class);
        postService = new PostService(postRepository, userRepository);
    }

    @Test
    void createPost_shouldSavePostWithCorrectData() {
        // given
        User user = User.builder()
                .id(1L)
                .username("tester")
                .email("test@example.com")
                .build();

        PostRequestDto dto = new PostRequestDto("title", "content", String.valueOf(user.getId()));

        PostEntity fakeSavedPost = PostEntity.builder()
                .id(1L)
                .title("title")
                .content("content")
                .author(user)
                .build();

        when(postRepository.save(any(PostEntity.class))).thenReturn(fakeSavedPost);

        // when
        Long postId = postService.create(dto, user);

        // then
        assertThat(postId).isEqualTo(1L);
        ArgumentCaptor<PostEntity> captor = ArgumentCaptor.forClass(PostEntity.class);
        verify(postRepository).save(captor.capture());
        PostEntity saved = captor.getValue();

        assertThat(saved.getTitle()).isEqualTo("title");
        assertThat(saved.getContent()).isEqualTo("content");
        assertThat(saved.getAuthor()).isEqualTo(user);
    }

    @Test
    void getAll_shouldReturnListOfPostResponseDto() {
        // given
        User user = User.builder()
                .id(1L)
                .username("tester")
                .email("test@example.com")
                .build();

        PostEntity post = PostEntity.builder()
                .id(1L)
                .title("title")
                .content("content")
                .author(user)
                .build();

        when(postRepository.findAll()).thenReturn(List.of(post));

        // when
        var result = postService.getAll(user);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).toString()).contains("title"); // indirect check
    }

    @Test
    void getById_shouldReturnPostResponseDtoAndIncreaseViewCount() {
        // given
        User user = User.builder()
                .id(1L)
                .username("tester")
                .email("test@example.com")
                .build();

        PostEntity post = PostEntity.builder()
                .id(1L)
                .title("title")
                .content("content")
                .author(user)
                .viewCount(0) // corrected to int
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        // when
        var result = postService.getById(1L, user);

        // then
        assertThat(result.toString()).contains("title"); // indirect check
        assertThat(post.getViewCount()).isEqualTo(1L);
    }

    @Test
    void updateByUser_shouldUpdatePostIfAuthorMatches() {
        // given
        User user = User.builder()
                .id(1L)
                .username("tester")
                .email("test@example.com")
                .build();

        PostEntity post = PostEntity.builder()
                .id(1L)
                .title("old")
                .content("old content")
                .author(user)
                .build();

        PostRequestDto dto = new PostRequestDto("new title", "new content", String.valueOf(user.getId()));

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        // when
        postService.updateByUser(1L, dto, user.getId());

        // then
        assertThat(post.getTitle()).isEqualTo("new title");
        assertThat(post.getContent()).isEqualTo("new content");
    }
}
