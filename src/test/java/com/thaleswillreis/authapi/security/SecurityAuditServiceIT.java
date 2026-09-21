package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.SecurityAuditEventRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class SecurityAuditServiceIT {

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

    @Autowired
    private SecurityAuditEventRepository securityAuditEventRepository;

    @Test
    void recordsLoginFailureEvent() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-audit-it"));
        userRepository.save(new User(tenant, "joao@acme-audit-it.com", passwordEncoder.encode("senha-correta")));

        String wrongPayload = "{\"email\":\"joao@acme-audit-it.com\",\"password\":\"senha-errada\"}";

        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content(wrongPayload))
                .andExpect(status().isUnauthorized());

        boolean hasFailureEvent = securityAuditEventRepository.findAll().stream()
                .anyMatch(event -> event.getEventType().name().equals("LOGIN_FAILURE")
                        && "joao@acme-audit-it.com".equals(event.getSubject())
                        && !event.isSuccess());

        assertThat(hasFailureEvent).isTrue();
    }

    @Test
    void recordsLoginSuccessEvent() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-audit-success-it"));
        userRepository
                .save(new User(tenant, "maria@acme-audit-success-it.com", passwordEncoder.encode("senha-correta")));

        String correctPayload = "{\"email\":\"maria@acme-audit-success-it.com\",\"password\":\"senha-correta\"}";

        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content(correctPayload))
                .andExpect(status().isOk());

        boolean hasSuccessEvent = securityAuditEventRepository.findAll().stream()
                .anyMatch(event -> event.getEventType().name().equals("LOGIN_SUCCESS")
                        && "maria@acme-audit-success-it.com".equals(event.getSubject())
                        && event.isSuccess());

        assertThat(hasSuccessEvent).isTrue();
    }

}