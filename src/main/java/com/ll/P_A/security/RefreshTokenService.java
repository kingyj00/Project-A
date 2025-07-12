package com.ll.P_A.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final long EXPIRATION = 60 * 60L; // 1시간

    public void save(String username, String refreshToken) {
        redisTemplate.opsForValue().set("refresh:" + username, refreshToken, EXPIRATION, TimeUnit.SECONDS);
    }

    public String get(String username) {
        Object token = redisTemplate.opsForValue().get("refresh:" + username);
        return token != null ? token.toString() : null;
    }

    public void delete(String username) {
        redisTemplate.delete("refresh:" + username);
    }
}