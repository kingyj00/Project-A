package com.ll.P_A.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final long DEFAULT_REFRESH_TTL_SEC = 30L * 24 * 60 * 60; // 30일

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public void storeActiveToken(String refreshToken,
                                 Long userId,
                                 String jti,
                                 String deviceId,
                                 Instant expiresAt) {

        String h = hash(refreshToken);
        String key = "refresh:" + h;

        Map<String, String> data = new HashMap<>();
        data.put("userId", String.valueOf(userId));
        data.put("jti", jti == null ? "" : jti);
        data.put("deviceId", deviceId == null ? "" : deviceId);
        data.put("status", "ACTIVE");

        long ttlSec;
        if (expiresAt != null) {
            long now = Instant.now().getEpochSecond();
            ttlSec = Math.max(60, expiresAt.getEpochSecond() - now); // 최소 60초 방어
            data.put("expiresAt", String.valueOf(expiresAt.getEpochSecond()));
        } else {
            ttlSec = DEFAULT_REFRESH_TTL_SEC;
            data.put("expiresAt", String.valueOf(Instant.now().plusSeconds(ttlSec).getEpochSecond()));
        }

        redisTemplate.opsForHash().putAll(key, data);
        redisTemplate.expire(key, Duration.ofSeconds(ttlSec));

        String userIdx = "user:" + userId + ":refresh";
        redisTemplate.opsForSet().add(userIdx, h);
        redisTemplate.expire(userIdx, Duration.ofSeconds(ttlSec));
    }

    public RefreshRecord findByToken(String refreshToken) {
        String h = hash(refreshToken);
        String key = "refresh:" + h;
        Map<Object, Object> m = redisTemplate.opsForHash().entries(key);
        if (m == null || m.isEmpty()) return null;

        return RefreshRecord.from(h, m);
    }

    public void markRotated(String hash) {
        String key = "refresh:" + hash;
        redisTemplate.opsForHash().put(key, "status", "ROTATED");
        // 회전된 토큰은 짧게만 보관(재사용 감지용). 예: 1시간
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    public void revokeByToken(String refreshToken) {
        String h = hash(refreshToken);
        String key = "refresh:" + h;
        redisTemplate.opsForHash().put(key, "status", "REVOKED");
        redisTemplate.expire(key, Duration.ofHours(1));
    }

    public void revokeAllForUser(Long userId) {
        String idx = "user:" + userId + ":refresh";
        Set<Object> hashes = redisTemplate.opsForSet().members(idx);
        if (hashes != null) {
            for (Object ho : hashes) {
                String h = String.valueOf(ho);
                String key = "refresh:" + h;
                redisTemplate.opsForHash().put(key, "status", "REVOKED");
                redisTemplate.expire(key, Duration.ofHours(1));
            }
        }
        redisTemplate.delete(idx);
    }

    @Deprecated
    public void save(String username, String refreshToken) {
        storeActiveToken(refreshToken, -1L, "", "legacy:" + username, null);
    }

    @Deprecated
    public String get(String username) {
        return null;
    }

    @Deprecated
    public void delete(String username) {
    }

    @Getter
    @AllArgsConstructor
    public static class RefreshRecord {
        private final String hash;
        private final Long userId;
        private final String jti;
        private final String deviceId;
        private final String status; // ACTIVE | ROTATED | REVOKED

        static RefreshRecord from(String hash, Map<Object, Object> m) {
            Long uid = null;
            Object uidObj = m.get("userId");
            if (uidObj != null) {
                try { uid = Long.valueOf(String.valueOf(uidObj)); } catch (NumberFormatException ignored) {}
            }
            return new RefreshRecord(
                    hash,
                    uid,
                    String.valueOf(m.getOrDefault("jti", "")),
                    String.valueOf(m.getOrDefault("deviceId", "")),
                    String.valueOf(m.getOrDefault("status", ""))
            );
        }

        public boolean isActive() { return "ACTIVE".equals(status); }
    }
}