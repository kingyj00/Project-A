package com.ll.P_A.security;

import jakarta.persistence.*;
import lombok.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

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

    @Builder.Default
    private boolean isAdmin = false;

    @Builder.Default
    private boolean enabled = false;

    @Column(length = 128)
    private String emailVerificationTokenHash; // SHA-256 hex

    private LocalDateTime emailVerificationExpiresAt;

    // 로그인 실패/잠금 관련
    private int loginFailCount;

    @Builder.Default
    private boolean accountNonLocked = true;

    private LocalDateTime lockTime;

    // --- 권한/상태 도메인 메서드 ---

    /** 명시적 접근자: 보안 어노테이션/권한 매핑에서 사용 */
    public boolean isAdmin() {
        return this.isAdmin;
    }

    /** 운영 편의를 위한 도메인 메서드(필요 시 사용) */
    public void grantAdmin() {
        this.isAdmin = true;
    }
    public void revokeAdmin() {
        this.isAdmin = false;
    }

    // --- 이메일 인증 ---

    public String generateVerificationToken() {
        String raw = randomUrlSafe(32); // 메일로 보낼 원문 토큰
        this.emailVerificationTokenHash = sha256Hex(raw);
        this.emailVerificationExpiresAt = LocalDateTime.now().plusHours(24);
        return raw;
    }

    public void verifyEmail() {
        this.enabled = true;
        this.emailVerificationTokenHash = null;
        this.emailVerificationExpiresAt = null;
    }

    public boolean isVerificationExpired() {
        return emailVerificationExpiresAt != null
                && emailVerificationExpiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isEmailVerified() {
        return enabled;
    }

    public void updatePassword(String newPassword) {
        this.password = newPassword;
    }

    public String changeEmail(String newEmail) {
        this.email = newEmail;
        this.enabled = false;
        return generateVerificationToken();
    }

    public void changeNickname(String newNickname) {
        this.nickname = newNickname;
    }

    // --- 계정 잠금 ---

    // 로그인 실패시 잠금처리
    public void increaseLoginFailCount() {
        this.loginFailCount++;
        if (this.loginFailCount >= 5) {
            this.accountNonLocked = false;
            this.lockTime = LocalDateTime.now();
        }
    }

    public void resetLoginFailCount() {
        this.loginFailCount = 0;
    }

    public void unlockIfTimePassed() {
        if (this.lockTime != null && this.lockTime.plusMinutes(30).isBefore(LocalDateTime.now())) {
            this.accountNonLocked = true;
            this.loginFailCount = 0;
            this.lockTime = null;
        }
    }

    public boolean isCurrentlyLocked() {
        return !this.accountNonLocked;
    }

    public long getLockRemainingSeconds() {
        if (this.lockTime == null) return 0;
        long seconds = Duration.between(LocalDateTime.now(), this.lockTime.plusMinutes(30)).getSeconds();
        return Math.max(seconds, 0);
    }

    // --- 유틸 ---

    private static String randomUrlSafe(int byteLen) {
        byte[] buf = new byte[byteLen];
        new SecureRandom().nextBytes(buf);
        // URL-safe, no padding
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
    }

    private static String sha256Hex(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes());
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}