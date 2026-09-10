package com.thaleswillreis.authapi;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class InfrastructureSmokeTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withDatabaseName("multitenant_auth_test")
                    .withUsername("auth_user")
                    .withPassword("test_password");

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @Test
    void postgresContainerIsUsable() throws Exception {
        try (Connection connection = DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1")) {

            resultSet.next();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }

    @Test
    void redisContainerRespondsToPing() throws Exception {
        var result = redis.execInContainer("redis-cli", "ping");
        assertThat(result.getStdout().trim()).isEqualTo("PONG");
    }

}