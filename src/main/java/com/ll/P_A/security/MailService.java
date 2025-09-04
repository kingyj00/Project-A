package com.ll.P_A.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MailService {

    @Value("${app.verify.base-url}")
    private String verifyBaseUrl;

    private final Environment env;

    public void sendVerificationEmail(User user, String rawToken) {
        if (verifyBaseUrl == null || verifyBaseUrl.isBlank()) {
            throw new IllegalStateException("Missing property 'app.verify.base-url'");
        }
        // URL-safe 인코딩
        String tokenParam = URLEncoder.encode(rawToken, StandardCharsets.UTF_8);
        String link = verifyBaseUrl + "/api/auth/verify-email?token=" + tokenParam;

        // 개발, 테스트 환경에서만 로그 출력
        if (env.acceptsProfiles("dev", "local", "test")) {
            System.out.printf("[MAIL] to=%s, subject=%s, link=%s%n",
                    user.getEmail(),
                    "이메일 인증을 완료해주세요",
                    link
            );
        }
    }

    @Deprecated
    public void sendVerificationEmail(User user) {
        throw new IllegalStateException("Use sendVerificationEmail(User user, String rawToken) instead.");
    }
}