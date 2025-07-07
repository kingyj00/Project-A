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
}