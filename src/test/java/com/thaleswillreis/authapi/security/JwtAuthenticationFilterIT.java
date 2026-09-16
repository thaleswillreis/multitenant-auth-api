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
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class JwtAuthenticationFilterIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Test
    void rejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRequestWithMalformedToken() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer token-invalido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsRefreshTokenOnProtectedEndpoint() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-jwt-it"));
        User user = userRepository.save(new User(tenant, "joao@acme-jwt-it.com", passwordEncoder.encode("senha123")));

        String refreshToken = jwtService.generateRefreshToken(user);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAccessAndScopesToTenantFromToken() throws Exception {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-scope-it"));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-scope-it"));

        User userA = userRepository.save(new User(tenantA, "joao@acme-scope-it.com", passwordEncoder.encode("senha123")));
        userRepository.save(new User(tenantB, "maria@beta-scope-it.com", passwordEncoder.encode("senha123")));

        String accessToken = jwtService.generateAccessToken(userA);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("joao@acme-scope-it.com"));
    }

    @Test
    void ignoresTenantHeaderWhenAuthenticatedViaToken() throws Exception {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-spoof-it"));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-spoof-it"));

        User userA = userRepository.save(new User(tenantA, "joao@acme-spoof-it.com", passwordEncoder.encode("senha123")));
        userRepository.save(new User(tenantB, "maria@beta-spoof-it.com", passwordEncoder.encode("senha123")));

        String accessToken = jwtService.generateAccessToken(userA);

        // tenta forjar acesso ao tenant B via header, mesmo autenticado com token do tenant A
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Tenant-Id", tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("joao@acme-spoof-it.com"));
    }

}