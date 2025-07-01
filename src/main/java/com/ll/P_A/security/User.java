package com.ll.P_A.security;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String nickname;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private boolean isAdmin = false;

    @Builder.Default
    private boolean enabled = false;

    private String emailVerificationToken;
    private LocalDateTime tokenGeneratedAt;
    private String refreshToken;

    // === 도메인 메서드 ===

    public void generateVerificationToken() {
        this.emailVerificationToken = UUID.randomUUID().toString();
        this.tokenGeneratedAt = LocalDateTime.now();
    }

    public void verifyEmail() {
        this.enabled = true;
        this.emailVerificationToken = null;
        this.tokenGeneratedAt = null;
    }

    public boolean isTokenExpired() {
        return tokenGeneratedAt != null && tokenGeneratedAt.isBefore(LocalDateTime.now().minusMinutes(30));
    }

    public boolean isEmailVerified() {
        return enabled;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public void changeEmail(String newEmail) {
        this.email = newEmail;
        this.enabled = false;
        generateVerificationToken();
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void removeRefreshToken() {
        this.refreshToken = null;
    }
}