package com.thaleswillreis.authapi.redis;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class RedisIntegrationIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void setsAndRetrievesValue() {
        redisTemplate.opsForValue().set("test:key", "test-value");

        String value = redisTemplate.opsForValue().get("test:key");

        assertThat(value).isEqualTo("test-value");
    }

    @Test
    void valueExpiresAfterTtl() throws InterruptedException {
        redisTemplate.opsForValue().set("test:expiring-key", "value", Duration.ofMillis(200));

        assertThat(redisTemplate.opsForValue().get("test:expiring-key")).isEqualTo("value");

        Thread.sleep(400);

        assertThat(redisTemplate.opsForValue().get("test:expiring-key")).isNull();
    }

}