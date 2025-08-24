package com.ll.P_A;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ll.P_A.security.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 통합 테스트(프로파일: test)
 * 시나리오
 *  1) 회원가입 → MailService mock 으로 토큰 캡처
 *  2) /api/auth/verify-email?token=... 호출 → 활성화
 *  3) /api/auth/login → access/refresh 획득
 *  4) 보호 API(/api/auth/me) OK
 *  5) /api/auth/reissue (Authorization 헤더에 "Bearer " 없이 refresh 그대로 전달) OK
 *  6) /api/auth/logout 후 /reissue 재시도 → 4xx
 *
 * 주의: 엔티티(User) 직접 참조/저장 없이, 공개 API만 사용.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class TokenFlowIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    //실제 Redis 대신 Mockito mock 등록(@MockitoBean)
    @MockitoBean
    StringRedisTemplate redisTemplate;

    //메일 발송 토큰 캡처용 mock
    @MockitoBean
    MailService mailService;

    private final Map<String, String> kv = new ConcurrentHashMap<>();
    private String capturedVerifyToken;

    @BeforeEach
    void setUpRedisAndMailMocks() {
        // --- Redis ValueOperations 최소 동작 흉내 ---
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> ops = mock(ValueOperations.class);

        doAnswer(inv -> { kv.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(ops).set(anyString(), anyString());

        doAnswer(inv -> { kv.put(inv.getArgument(0), inv.getArgument(1)); return null; })
                .when(ops).set(anyString(), anyString(), any(Duration.class));

        when(ops.get(anyString())).thenAnswer((Answer<String>) inv -> kv.get(inv.getArgument(0)));

        when(redisTemplate.opsForValue()).thenReturn(ops);
        when(redisTemplate.delete(anyString()))
                .thenAnswer((Answer<Boolean>) inv -> kv.remove(inv.getArgument(0)) != null);
        when(redisTemplate.expire(anyString(), any(Duration.class))).thenReturn(true);

        // --- 메일 토큰 캡처 ---
        doAnswer(inv -> {
            // MailService.sendVerificationEmail(User, String)
            // 0번째 arg: User (타입은 신경 안 씀), 1번째 arg: rawToken
            capturedVerifyToken = inv.getArgument(1, String.class);
            return null;
        }).when(mailService).sendVerificationEmail(any(), anyString());
    }

    private record SignupPayload(String username, String password, String email, String nickname) {}
    private record LoginPayload(String username, String password) {}

    @Test
    @DisplayName("가입→이메일인증→로그인→보호API→재발급→로그아웃 후 재발급 실패")
    void fullFlow() throws Exception {
        // 1) 회원가입
        mvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(
                                new SignupPayload("user1", "pass1!", "user1@example.com", "nick1")
                        )))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        // 2) 이메일 인증 (MailService mock에서 토큰 캡처)
        mvc.perform(get("/api/auth/verify-email").param("token", capturedVerifyToken))
                .andExpect(status().isOk());

        // 3) 로그인
        var loginRes = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(new LoginPayload("user1", "pass1!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode tokens1 = om.readTree(loginRes.getResponse().getContentAsString());
        String access1  = tokens1.get("accessToken").asText();
        String refresh1 = tokens1.get("refreshToken").asText();

        // 4) 보호 API 호출
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access1))
                .andExpect(status().isOk());

        // 5) 리프레시로 재발급
        // UserController.reissue는 Authorization 헤더의 "원문" 문자열을 파라미터로 받으므로
        // 여기서는 "Bearer " 접두사 없이 refresh 토큰 자체만 넘김.
        var reissueRes = mvc.perform(post("/api/auth/reissue")
                        .header(HttpHeaders.AUTHORIZATION, refresh1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        JsonNode tokens2 = om.readTree(reissueRes.getResponse().getContentAsString());
        String access2  = tokens2.get("accessToken").asText();
        String refresh2 = tokens2.get("refreshToken").asText();

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + access2))
                .andExpect(status().isOk());

        // 6) 로그아웃 → 이후 재발급 실패 확인
        mvc.perform(post("/api/auth/logout").header(HttpHeaders.AUTHORIZATION, "Bearer " + access2))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/reissue").header(HttpHeaders.AUTHORIZATION, refresh2))
                .andExpect(status().is4xxClientError());
    }
}