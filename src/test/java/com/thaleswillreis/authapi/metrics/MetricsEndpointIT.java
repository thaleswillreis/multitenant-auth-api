package com.thaleswillreis.authapi.metrics;

import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.repository.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureObservability
@AutoConfigureMockMvc
@Testcontainers
class MetricsEndpointIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Container
    @ServiceConnection(name = "redis")
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void rateLimitProperties(DynamicPropertyRegistry registry) {
        registry.add("app.rate-limit.capacity", () -> "3");
        registry.add("app.rate-limit.refill-duration-seconds", () -> "60");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Test
    void healthEndpointIsPubliclyAccessible() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("\"status\":\"UP\"")));
    }

    @Test
    void prometheusEndpointExposesSecurityAndRateLimitMetrics() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-metrics-it"));

        mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content("{\"email\":\"ninguem@acme-metrics-it.com\",\"password\":\"errada\"}"));

        // Simula um cliente diferente (IP proprio) para esgotar o limite,
        // sem afetar o "scraper" que vai consultar /actuator/prometheus depois
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(get("/api/users").header("X-Forwarded-For", "203.0.113.50"));
        }

        String prometheusOutput = mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(prometheusOutput).contains("security_events_total");
        assertThat(prometheusOutput).contains("rate_limit_rejections_total");
    }

}