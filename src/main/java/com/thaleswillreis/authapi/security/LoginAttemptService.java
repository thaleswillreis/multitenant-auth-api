package com.thaleswillreis.authapi.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class LoginAttemptService {

    private static final String KEY_PREFIX = "login-attempts:";
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;

    public LoginAttemptService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isLocked(UUID tenantId, String email) {
        String value = redisTemplate.opsForValue().get(buildKey(tenantId, email));
        if (value == null) {
            return false;
        }
        return Long.parseLong(value) >= MAX_ATTEMPTS;
    }

    public void recordFailure(UUID tenantId, String email) {
        String key = buildKey(tenantId, email);
        Long attempts = redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {
            redisTemplate.expire(key, LOCKOUT_DURATION);
        }
    }

    public void recordSuccess(UUID tenantId, String email) {
        redisTemplate.delete(buildKey(tenantId, email));
    }

    private String buildKey(UUID tenantId, String email) {
        return KEY_PREFIX + tenantId + ":" + email.toLowerCase();
    }

}