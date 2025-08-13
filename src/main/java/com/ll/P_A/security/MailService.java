package com.ll.P_A.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService {

    // 인증 링크가 연결될 프런트/백엔드의 베이스 URL
    @Value("${app.verify.base-url:https://example.com}")
    private String verifyBaseUrl;

    public void sendVerificationEmail(User user, String rawToken) {
        // 토큰은 URL 인코딩
        String tokenParam = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String link = verifyBaseUrl + "/api/auth/verify-email?token=" + tokenParam;

        System.out.printf("[MAIL] to=%s, subject=%s, link=%s%n",
                user.getEmail(),
                "이메일 인증을 완료해주세요",
                link
        );
    }

    @Deprecated
    public void sendVerificationEmail(User user) {
        throw new IllegalStateException("Use sendVerificationEmail(User user, String rawToken) instead.");
    }
}