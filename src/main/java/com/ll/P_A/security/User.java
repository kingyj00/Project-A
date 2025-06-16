package com.ll.P_A.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username; // 아이디

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean isAdmin = false;

    // 이메일 인증 상태
    @Builder.Default
    private boolean enabled = false;

    private String emailVerificationToken; // 인증 토큰

    private LocalDateTime tokenGeneratedAt; // 토큰 생성 시간

    // 이메일 인증 토큰 생성
    public void generateVerificationToken() {
        this.emailVerificationToken = UUID.randomUUID().toString();
        this.tokenGeneratedAt = LocalDateTime.now();
    }

    // 토큰 유효성 검증
    public boolean isValidToken(String token) {
        return this.emailVerificationToken != null && this.emailVerificationToken.equals(token);
    }

    // 이메일 인증 처리
    public void verifyEmail() {
        this.enabled = true;
        this.emailVerificationToken = null;
        this.tokenGeneratedAt = null;
    }

    // 명시적 메서드 추가: isEmailVerified / setEmailVerified
    public boolean isEmailVerified() {
        return this.enabled;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.enabled = emailVerified;
    }

    public boolean isTokenExpired() {
        return tokenGeneratedAt != null && tokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(30));
    }// 30분안에 인증 안하면 만료
}