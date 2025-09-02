package com.ll.P_A;

import com.ll.P_A.post.PostController;
import com.ll.P_A.post.PostService;
import com.ll.P_A.security.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean; // ※ 3.4 경고가 떠도 동작은 합니다.
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostController.class)
@AutoConfigureMockMvc(addFilters = false)
class TokenFlowIntegrationTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    PostService postService;

    @MockBean
    UserService userService;

    @Test
    @DisplayName("GET /api/posts → 200 OK (빈 페이지)")
    void getPosts_ok() throws Exception {
        // 컨트롤러가 postService.getAll(loginUser, pageable, keyword) 호출하므로 3개 인자 매칭
        when(postService.getAll(any(), any(Pageable.class), nullable(String.class)))
                .thenReturn(Page.empty());

        mvc.perform(get("/api/posts")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }
}