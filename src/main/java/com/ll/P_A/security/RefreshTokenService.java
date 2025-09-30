package com.ll.P_A.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RefreshTokenService {

    /** 주입되지 않을 수 있음(테스트/로컬 등). 있으면 Redis 사용, 없으면 In-Memory 폴백 */
    private final RedisTemplate<String, Object> redisTemplate;
    private final boolean useRedis;

    // In-Memory fallback 저장소 (Redis 미사용 시)
    private final Map<String, Map<String, String>> memHash = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> memSet = new ConcurrentHashMap<>();
    private final Map<String, Long> memTtl = new ConcurrentHashMap<>();

    private static final long DEFAULT_REFRESH_TTL_SEC = 30L * 24 * 60 * 60; // 30일

    public RefreshTokenService(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.useRedis = (this.redisTemplate != null);
    }

    /* ===================== 유틸 ===================== */

    private String hash(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private long nowEpoch() {
        return Instant.now().getEpochSecond();
    }

    /* ===================== 저장/조회 ===================== */

    /**
     * ACTIVE 상태의 Refresh 토큰 저장
     * - user 인덱스, family 인덱스에 해시를 같이 등록
     */
    public void storeActiveToken(String refreshToken,
                                 Long userId,
                                 String jti,
                                 String deviceId,
                                 String familyId,
                                 Instant expiresAt) {

        String h = hash(refreshToken);
        String key = "refresh:" + h;

        Map<String, String> data = new HashMap<>();
        data.put("userId", String.valueOf(userId));
        data.put("jti", jti == null ? "" : jti);
        data.put("deviceId", deviceId == null ? "" : deviceId);
        data.put("familyId", familyId == null ? "" : familyId);
        data.put("status", "ACTIVE");

        long ttlSec;
        if (expiresAt != null) {
            long ttl = Math.max(60, expiresAt.getEpochSecond() - nowEpoch()); // 최소 60초 방어
            ttlSec = ttl;
            data.put("expiresAt", String.valueOf(expiresAt.getEpochSecond()));
        } else {
            ttlSec = DEFAULT_REFRESH_TTL_SEC;
            data.put("expiresAt", String.valueOf(Instant.now().plusSeconds(ttlSec).getEpochSecond()));
        }

        // 해시 저장
        hPutAll(key, data);
        expire(key, Duration.ofSeconds(ttlSec));

        // 사용자 인덱스 세트 저장
        String userIdx = "user:" + userId + ":refresh";
        sAdd(userIdx, h);
        expire(userIdx, Duration.ofSeconds(ttlSec));

        // 가족(family) 인덱스 세트 저장 (로테이션/재사용 대응)
        if (familyId != null && !familyId.isBlank()) {
            String famIdx = "family:" + familyId + ":refresh";
            sAdd(famIdx, h);
            expire(famIdx, Duration.ofSeconds(ttlSec));
        }
    }

    /**
     * 해시 키에 저장된 메타를 조회
     */
    public RefreshRecord findByToken(String refreshToken) {
        String h = hash(refreshToken);
        return findByHash(h);
    }

    /**
     * 이미 해시값을 알고 있을 때(내부용)
     */
    public RefreshRecord findByHash(String hash) {
        String key = "refresh:" + hash;
        Map<Object, Object> m = hEntries(key);
        if (m == null || m.isEmpty()) return null;
        return RefreshRecord.from(hash, m);
    }

    /* ===================== 상태 변경 ===================== */

    /**
     * 로테이션 처리: 이전 토큰은 ROTATED로 표시(재사용 감지용으로 잠시 보관)
     */
    public void markRotatedByHash(String hash) {
        String key = "refresh:" + hash;
        hPut(key, "status", "ROTATED");
        expire(key, Duration.ofHours(1)); // 재사용 감지 위해 짧게 보관
    }

    public void markRotatedByToken(String refreshToken) {
        markRotatedByHash(hash(refreshToken));
    }

    /**
     * 단일 토큰 폐기(REVOKED) - 재사용 차단
     */
    public void revokeByToken(String refreshToken) {
        revokeByHash(hash(refreshToken));
    }

    public void revokeByHash(String hash) {
        String key = "refresh:" + hash;
        hPut(key, "status", "REVOKED");
        expire(key, Duration.ofHours(1)); // 짧은 보관
    }

    /**
     * 특정 사용자(userId)의 모든 Refresh 폐기
     */
    public void revokeAllForUser(Long userId) {
        String idx = "user:" + userId + ":refresh";
        Set<Object> hashes = sMembers(idx);
        if (hashes != null) {
            for (Object ho : hashes) {
                String h = String.valueOf(ho);
                String key = "refresh:" + h;
                hPut(key, "status", "REVOKED");
                expire(key, Duration.ofHours(1));
            }
        }
        deleteKey(idx);
    }

    /**
     * 특정 familyId의 모든 Refresh 폐기(재사용 공격 탐지 시 사용)
     */
    public void revokeFamily(String familyId) {
        if (familyId == null || familyId.isBlank()) return;
        String idx = "family:" + familyId + ":refresh";
        Set<Object> hashes = sMembers(idx);
        if (hashes != null) {
            for (Object ho : hashes) {
                String h = String.valueOf(ho);
                String key = "refresh:" + h;
                hPut(key, "status", "REVOKED");
                expire(key, Duration.ofHours(1));
            }
        }
        deleteKey(idx);
    }

    /* ===================== 구(호환) API ===================== */

    @Deprecated
    public void save(String username, String refreshToken) {
        storeActiveToken(refreshToken, -1L, "", "legacy:" + username, null, null);
    }

    @Deprecated
    public String get(String username) { return null; }

    @Deprecated
    public void delete(String username) { } // 외부 API 보존(비사용)

    /* ===================== 레코드 ===================== */

    @Getter
    @AllArgsConstructor
    public static class RefreshRecord {
        private final String hash;
        private final Long userId;
        private final String jti;
        private final String deviceId;
        private final String familyId;
        private final String status;    // ACTIVE | ROTATED | REVOKED
        private final Long expiresAt;   // epoch seconds (nullable)

        static RefreshRecord from(String hash, Map<Object, Object> m) {
            Long uid = null;
            Object uidObj = m.get("userId");
            if (uidObj != null) {
                try { uid = Long.valueOf(String.valueOf(uidObj)); } catch (NumberFormatException ignored) {}
            }
            Long exp = null;
            Object expObj = m.get("expiresAt");
            if (expObj != null) {
                try { exp = Long.valueOf(String.valueOf(expObj)); } catch (NumberFormatException ignored) {}
            }
            return new RefreshRecord(
                    hash,
                    uid,
                    String.valueOf(m.getOrDefault("jti", "")),
                    String.valueOf(m.getOrDefault("deviceId", "")),
                    String.valueOf(m.getOrDefault("familyId", "")),
                    String.valueOf(m.getOrDefault("status", "")),
                    exp
            );
        }

        public boolean isActive()  { return "ACTIVE".equals(status); }
        public boolean isRotated() { return "ROTATED".equals(status); }
        public boolean isRevoked() { return "REVOKED".equals(status); }
        public boolean isExpired() {
            return (expiresAt != null) && (expiresAt <= Instant.now().getEpochSecond());
        }
    }

    /* ===================== In-Memory/Redis 헬퍼 ===================== */

    private void hPutAll(String key, Map<String, String> value) {
        if (useRedis) {
            redisTemplate.opsForHash().putAll(key, value);
        } else {
            checkExpiry(key);
            memHash.put(key, new HashMap<>(value));
        }
    }

    private void hPut(String key, String field, String value) {
        if (useRedis) {
            redisTemplate.opsForHash().put(key, field, value);
        } else {
            checkExpiry(key);
            memHash.computeIfAbsent(key, k -> new HashMap<>()).put(field, value);
        }
    }

    private Map<Object, Object> hEntries(String key) {
        if (useRedis) {
            return redisTemplate.opsForHash().entries(key);
        } else {
            if (isExpired(key)) return Collections.emptyMap();
            Map<String, String> map = memHash.get(key);
            if (map == null) return Collections.emptyMap();
            return new HashMap<>(map);
        }
    }

    private void sAdd(String key, String member) {
        if (useRedis) {
            redisTemplate.opsForSet().add(key, member);
        } else {
            checkExpiry(key);
            memSet.computeIfAbsent(key, k -> ConcurrentHashMap.newKeySet()).add(member);
        }
    }

    private Set<Object> sMembers(String key) {
        if (useRedis) {
            Set<Object> s = redisTemplate.opsForSet().members(key);
            return (s == null) ? Collections.emptySet() : s;
        } else {
            if (isExpired(key)) return Collections.emptySet();
            Set<String> s = (Set<String>) (Set<?>) memSet.get(key);
            if (s == null) return Collections.emptySet();
            return new HashSet<>(s);
        }
    }

    private void expire(String key, Duration duration) {
        if (useRedis) {
            redisTemplate.expire(key, duration);
        } else {
            memTtl.put(key, System.currentTimeMillis() + duration.toMillis());
        }
    }

    // 이름 변경: 내부 헬퍼 충돌 회피
    private void deleteKey(String key) {
        if (useRedis) {
            redisTemplate.delete(key);
        } else {
            memHash.remove(key);
            memSet.remove(key);
            memTtl.remove(key);
        }
    }

    private void checkExpiry(String key) {
        if (isExpired(key)) {
            memHash.remove(key);
            memSet.remove(key);
            memTtl.remove(key);
        }
    }

    private boolean isExpired(String key) {
        Long until = memTtl.get(key);
        return until != null && until <= System.currentTimeMillis();
    }
}