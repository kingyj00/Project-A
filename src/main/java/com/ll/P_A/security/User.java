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

    private boolean enabled = false; // 계정 활성화 여부 (메일 인증 여부)

    private String emailVerificationToken; // 인증 토큰

    private LocalDateTime tokenGeneratedAt; // 토큰 생성 시간

    public void generateVerificationToken() {
        this.emailVerificationToken = UUID.randomUUID().toString();
        this.tokenGeneratedAt = LocalDateTime.now();
    }

    public boolean isValidToken(String token) {
        return this.emailVerificationToken != null && this.emailVerificationToken.equals(token);
    }

    public void verifyEmail() {
        this.enabled = true;
        this.emailVerificationToken = null;
        this.tokenGeneratedAt = null;
    }
}