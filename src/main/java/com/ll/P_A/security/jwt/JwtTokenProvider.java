package com.ll.P_A.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final Key secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,                           // 비밀키: 기본값 없음(환경/프로파일에서 주입)
            @Value("${jwt.access-validity-ms:900000}") long accessTtlMs,     // 기본 15분 = 900,000 ms
            @Value("${jwt.refresh-validity-ms:2592000000}") long refreshTtlMs // 기본 30일 = 2,592,000,000 ms
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMs = accessTtlMs;
        this.refreshTokenValidityMs = refreshTtlMs;
    }

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

    private Claims parse(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 공통: 사용자 이름 추출
    public String getUsernameFromToken(String token) {
        try { return parse(token).getSubject(); }
        catch (Exception e) { return null; }
    }

    // 공통: jti 추출 (refresh에서 사용)
    public String getJti(String token) {
        try { return parse(token).getId(); }
        catch (Exception e) { return null; }
    }

    // 공통: 만료시간
    public Date getExpiration(String token) {
        return parse(token).getExpiration();
    }

    // 공통: 토큰 타입 (access/refresh)
    public String getTokenType(String token) {
        try { return parse(token).get("typ", String.class); }
        catch (Exception e) { return null; }
    }

    // 공통: 디바이스 ID (refresh에서 사용)
    public String getDeviceId(String token) {
        try { return parse(token).get("did", String.class); }
        catch (Exception e) { return null; }
    }

    // 유효성 검증
    public boolean validateToken(String token) {
        try { parse(token); return true; }
        catch (JwtException | IllegalArgumentException e) { return false; }
    }

    public record RefreshPayload(String token, String jti, Date expiresAt) {}
}