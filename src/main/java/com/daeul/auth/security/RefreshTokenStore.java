package com.daeul.auth.security;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private final StringRedisTemplate redisTemplate;
    private static final long REFRESH_TOKEN_TTL = 7 * 24 * 60 * 60; // 7일 (초)

    public void saveToken(String email, String refreshToken) {
        redisTemplate.opsForValue().set("refresh:" + email, refreshToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
    }

    public String getToken(String email) {
        return redisTemplate.opsForValue().get("refresh:" + email);
    }

    public boolean validateToken(String email, String refreshToken) {
        String stored = getToken(email);
        return stored != null && stored.equals(refreshToken);
    }

    public void removeToken(String email) {
        redisTemplate.delete("refresh:" + email);
    }
}
