package com.thaleswillreis.authapi.security;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenBlacklistService {

    private static final String KEY_PREFIX = "blacklist:";

    private final StringRedisTemplate redisTemplate;

    public TokenBlacklistService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklist(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);

        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }

        redisTemplate.opsForValue().set(KEY_PREFIX + jti, "revoked", ttl);
    }

    public boolean isBlacklisted(String jti) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(KEY_PREFIX + jti));
    }

}