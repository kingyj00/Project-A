package com.ll.P_A.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final Key secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    // 시계 오차 허용 (초). 분산 환경/컨테이너 시간 차 예방 (왜 하는지 공부)
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secretRaw,
            @Value("${jwt.secret-base64:}") String secretBase64,
            @Value("${jwt.access-token-validity-ms:900000}") long accessTtlMs,
            @Value("${jwt.refresh-token-validity-ms:2592000000}") long refreshTtlMs
    ) {
        byte[] keyBytes;

        if (!isBlank(secretBase64)) {
            try {
                keyBytes = Base64.getDecoder().decode(secretBase64);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException(
                        "jwt.secret-base64 값이 Base64 형식이 아닙니다. 설정 값에 공백/오타가 없는지 확인해 주세요.", e
                );
            }
        } else if (!isBlank(secretRaw)) {
            keyBytes = secretRaw.getBytes(StandardCharsets.UTF_8);
        } else {
            throw new IllegalStateException(
                    "JWT 비밀키가 설정되지 않았습니다. 환경변수(JWT_SECRET 또는 JWT_SECRET_BASE64)나 application.yml을 확인해 주세요."
            );
        }

        try {
            this.secretKey = Keys.hmacShaKeyFor(keyBytes); // HS256 최소 32바이트
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "JWT 비밀키가 너무 짧습니다. HS256 사용 시 최소 32바이트(영문 기준 32자 이상)를 권장합니다.", e
            );
        }

        this.accessTokenValidityMs = accessTtlMs;
        this.refreshTokenValidityMs = refreshTtlMs;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // ===== 발급 =====

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", "access")
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public RefreshPayload generateRefreshToken(String username, String deviceId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + refreshTokenValidityMs);
        String jti = UUID.randomUUID().toString();

        String token = Jwts.builder()
                .setSubject(username)
                .setId(jti)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", "refresh")
                .claim("did", deviceId == null ? "" : deviceId)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();

        return new RefreshPayload(token, jti, exp);
    }

    // ===== 파싱/검증 =====
    // 시계 오차 허용? (왜 하는지 공부 하기)
    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsernameFromToken(String token) {
        try {
            return parse(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    public String getJti(String token) {
        try {
            return parse(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    public Date getExpiration(String token) {
        try {
            return parse(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    public String getTokenType(String token) {
        try {
            return parse(token).get("typ", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public String getDeviceId(String token) {
        try {
            return parse(token).get("did", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw e; // 만료는 그대로 던져 필터에서 "만료"로 구분 처리
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 나머지는 무효로 판단
        }
    }

    // Value Object
    public record RefreshPayload(String token, String jti, Date expiresAt) {}
}