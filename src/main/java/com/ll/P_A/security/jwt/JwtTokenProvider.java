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

    public JwtTokenProvider(
            @Value("${jwt.secret:}") String secretRaw,
            @Value("${jwt.secret-base64:}") String secretBase64,
            @Value("${jwt.access-token-validity-ms:900000}") long accessTtlMs,
            @Value("${jwt.refresh-token-validity-ms:2592000000}") long refreshTtlMs
    ) {
        // 키 바이트 결정: Base64 > 일반 문자열 순으로 사용
        byte[] keyBytes;

        if (!isBlank(secretBase64)) {
            try {
                keyBytes = Base64.getDecoder().decode(secretBase64);
            } catch (IllegalArgumentException e) {
                // 사용자 친화적 메시지로 변경
                throw new IllegalStateException(
                        "jwt.secret-base64 값이 Base64 형식이 아닙니다. 설정 값에 공백/오타가 없는지 확인해 주세요.",
                        e
                );
            }
        } else if (!isBlank(secretRaw)) {
            keyBytes = secretRaw.getBytes(StandardCharsets.UTF_8);
        } else {
            // 사용자 친화적 메시지로 변경
            throw new IllegalStateException(
                    "JWT 비밀키가 설정되지 않았습니다. 환경변수(JWT_SECRET 또는 JWT_SECRET_BASE64)나 application.yml을 확인해 주세요."
            );
        }

        try {
            // HS256은 최소 256bit(32바이트) 이상 필요
            this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            // 사용자 친화적 메시지로 변경
            throw new IllegalStateException(
                    "JWT 비밀키가 너무 짧습니다. HS256 사용 시 최소 32바이트(영문 기준 32자 이상)를 권장합니다.",
                    e
            );
        }

        this.accessTokenValidityMs = accessTtlMs;
        this.refreshTokenValidityMs = refreshTtlMs;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    // 토큰 생성
    // Access Token 생성 (subject = username)
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

    // Refresh Token 생성 (subject = username, did = deviceId, jti 포함)
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

    // 파싱/검증

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 사용자명 추출
    public String getUsernameFromToken(String token) {
        try {
            return parse(token).getSubject();
        } catch (Exception e) {
            return null;
        }
    }

    // jti 추출 (refresh 전용)
    public String getJti(String token) {
        try {
            return parse(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    // 만료시간 추출
    public Date getExpiration(String token) {
        try {
            return parse(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }

    // 토큰 타입(access/refresh)
    public String getTokenType(String token) {
        try {
            return parse(token).get("typ", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 디바이스 ID (refresh)
    public String getDeviceId(String token) {
        try {
            return parse(token).get("did", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    // 서명/만료 포함 유효성 검증
    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // Value Object : 값 그 자체를 표현하는 객체. ID가 아니라 내용이 같으면 같은 객체로 취급

    public record RefreshPayload(String token, String jti, Date expiresAt) {}
}