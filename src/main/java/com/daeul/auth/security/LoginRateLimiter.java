package com.daeul.auth.security;

import com.daeul.auth.exception.TooManyLoginAttemptsException;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginRateLimiter {
    private final StringRedisTemplate redisTemplate;
    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_SECONDS = 60;

    public void checkRateLimit(String ip) {
        String key = "login:ip:" + ip;
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == 1) {
            redisTemplate.expire(key, WINDOW_SECONDS, TimeUnit.SECONDS);
        }

        if (count != null && count > MAX_ATTEMPTS) {
            throw new TooManyLoginAttemptsException("로그인 시도가 너무 많습니다. 잠시 후 다시 시도하세요.");
        }
    }
}

