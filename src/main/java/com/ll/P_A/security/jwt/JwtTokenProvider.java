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

    private final Key secretKey;           // 현재 서명 키 (발급 및 1차 검증)
    private final Key previousSecretKey;   // 이전 서명 키 (2차 검증; 없으면 null)
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    // 시계 오차 허용(초): 분산 환경/컨테이너 시간 차 예방
    private static final long ALLOWED_CLOCK_SKEW_SECONDS = 30;

    public JwtTokenProvider(
            // 현재 키(기존 키와 호환 유지)
            @Value("${jwt.secret:}") String secretRaw,
            @Value("${jwt.secret-base64:}") String secretBase64,

            // 이전 키(선택)
            @Value("${jwt.previous-secret:}") String prevSecretRaw,
            @Value("${jwt.previous-secret-base64:}") String prevSecretBase64,

            @Value("${jwt.access-token-validity-ms:900000}") long accessTtlMs,
            @Value("${jwt.refresh-token-validity-ms:2592000000}") long refreshTtlMs
    ) {
        this.secretKey = buildKeyOrThrow(secretRaw, secretBase64, true);
        this.previousSecretKey = buildKeyOrNull(prevSecretRaw, prevSecretBase64);
        this.accessTokenValidityMs = accessTtlMs;
        this.refreshTokenValidityMs = refreshTtlMs;
    }

    /* ===================== 키 빌더 ===================== */

    private Key buildKeyOrThrow(String raw, String b64, boolean required) {
        byte[] keyBytes = toBytes(raw, b64, required);
        try {
            return Keys.hmacShaKeyFor(keyBytes); // HS256 최소 32바이트 권장
        } catch (WeakKeyException e) {
            throw new IllegalStateException(
                    "JWT 비밀키가 너무 짧습니다. HS256 사용 시 최소 32바이트(영문 기준 32자 이상) 권장.", e
            );
        }
    }

    private Key buildKeyOrNull(String raw, String b64) {
        if (isBlank(raw) && isBlank(b64)) return null;
        byte[] keyBytes = toBytes(raw, b64, false);
        try {
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (WeakKeyException e) {
            // 이전 키는 선택 항목이므로 설정 오류 시 무시하고 null 처리
            return null;
        }
    }

    private byte[] toBytes(String raw, String b64, boolean required) {
        if (!isBlank(b64)) {
            try {
                return Base64.getDecoder().decode(b64);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("jwt.secret-base64 형식 오류(Base64 아님).", e);
            }
        } else if (!isBlank(raw)) {
            return raw.getBytes(StandardCharsets.UTF_8);
        } else if (required) {
            throw new IllegalStateException(
                    "JWT 비밀키가 설정되지 않았습니다. 환경변수(JWT_SECRET/JWT_SECRET_BASE64) 또는 application.yml 확인."
            );
        } else {
            return null;
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    /* ===================== 발급 ===================== */

    public String generateAccessToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + accessTokenValidityMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .claim("typ", "access")
                .signWith(secretKey, SignatureAlgorithm.HS256) //항상 현재 키로 서명
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
                .signWith(secretKey, SignatureAlgorithm.HS256) //항상 현재 키로 서명
                .compact();

        return new RefreshPayload(token, jti, exp);
    }

    /* ===================== 파싱/검증 ===================== */

    /** 현재 키로 검증 실패 시, 이전 키가 설정돼 있으면 한 번 더 검증 시도 */
    private Claims parse(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 만료는 그대로 던져서 상위에서 "만료"로 구분
            throw e;
        } catch (JwtException | IllegalArgumentException primaryFail) {
            // 서명 불일치 등으로 실패 → 이전 키로 한 번 더 시도
            if (previousSecretKey == null) throw primaryFail;
            try {
                return Jwts.parserBuilder()
                        .setSigningKey(previousSecretKey)
                        .setAllowedClockSkewSeconds(ALLOWED_CLOCK_SKEW_SECONDS)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
            } catch (JwtException | IllegalArgumentException secondaryFail) {
                // 둘 다 실패면 원인 그대로 반환(무효)
                throw secondaryFail;
            }
        }
    }

    public String getUsernameFromToken(String token) {
        try { return parse(token).getSubject(); } catch (Exception e) { return null; }
    }

    public String getJti(String token) {
        try { return parse(token).getId(); } catch (Exception e) { return null; }
    }

    public Date getExpiration(String token) {
        try { return parse(token).getExpiration(); } catch (Exception e) { return null; }
    }

    public String getTokenType(String token) {
        try { return parse(token).get("typ", String.class); } catch (Exception e) { return null; }
    }

    public String getDeviceId(String token) {
        try { return parse(token).get("did", String.class); } catch (Exception e) { return null; }
    }

    public boolean validateToken(String token) {
        try {
            parse(token);
            return true;
        } catch (ExpiredJwtException e) {
            throw e; // 만료는 위로 전달
        } catch (JwtException | IllegalArgumentException e) {
            return false; // 무효
        }
    }

    // Value Object
    public record RefreshPayload(String token, String jti, Date expiresAt) {}
}