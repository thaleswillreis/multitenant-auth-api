package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class LoginAttemptServiceIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void locksAccountAfterFiveFailedAttempts() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-brute-force-it"));
        userRepository
                .save(new User(tenant, "vitima@acme-brute-force-it.com", passwordEncoder.encode("senha-correta")));

        String wrongLoginPayload = "{\"email\":\"vitima@acme-brute-force-it.com\",\"password\":\"senha-errada\"}";
        String correctLoginPayload = "{\"email\":\"vitima@acme-brute-force-it.com\",\"password\":\"senha-correta\"}";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                    .header("X-Tenant-Id", tenant.getId().toString())
                    .contentType("application/json")
                    .content(wrongLoginPayload))
                    .andExpect(status().isUnauthorized());
        }

        // mesmo com a senha CORRETA agora, a conta ja esta bloqueada
        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content(correctLoginPayload))
                .andExpect(status().isUnauthorized());
    }

}