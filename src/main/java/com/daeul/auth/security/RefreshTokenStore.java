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
    private final String REFRESH_TOKEN_PREFIX = "refresh:";

    public void saveToken(Long id, String refreshToken) {
        redisTemplate.opsForValue().set(REFRESH_TOKEN_PREFIX + id, refreshToken, REFRESH_TOKEN_TTL, TimeUnit.SECONDS);
    }

    public String getToken(Long id) {
        return redisTemplate.opsForValue().get(REFRESH_TOKEN_PREFIX + id);
    }

    public boolean validateToken(Long id, String refreshToken) {
        String stored = getToken(id);
        return stored != null && stored.equals(refreshToken);
    }

    public void removeToken(Long id) {
        redisTemplate.delete(REFRESH_TOKEN_PREFIX + id);
    }
}
