package com.thaleswillreis.authapi.security;

import com.jayway.jsonpath.JsonPath;
import com.thaleswillreis.authapi.model.Role;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.RoleRepository;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CrossTenantAndDataExposureIT {

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
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void crossTenantTokenCannotSeeOtherTenantsUsers() throws Exception {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-crosstenant-" + UUID.randomUUID()));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-crosstenant-" + UUID.randomUUID()));

        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        User adminA = new User(tenantA, "admin@tenant-a.com", passwordEncoder.encode("senha123"));
        adminA.addRole(adminRole);
        userRepository.save(adminA);

        User userB = new User(tenantB, "vitima@tenant-b.com", passwordEncoder.encode("senha123"));
        userRepository.save(userB);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenantA.getId().toString())
                .contentType("application/json")
                .content("{\"email\":\"admin@tenant-a.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String tokenA = JsonPath.read(loginResponse, "$.accessToken");

        String usersResponse = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(usersResponse).doesNotContain("vitima@tenant-b.com");
        assertThat(usersResponse).contains("admin@tenant-a.com");
    }

    @Test
    void xTenantIdHeaderIsIgnoredOnceAuthenticated() throws Exception {
        Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-header-ignored-" + UUID.randomUUID()));
        Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-header-ignored-" + UUID.randomUUID()));

        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        User adminA = new User(tenantA, "admin@tenant-a-header.com", passwordEncoder.encode("senha123"));
        adminA.addRole(adminRole);
        userRepository.save(adminA);

        User userB = new User(tenantB, "vitima@tenant-b-header.com", passwordEncoder.encode("senha123"));
        userRepository.save(userB);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenantA.getId().toString())
                .contentType("application/json")
                .content("{\"email\":\"admin@tenant-a-header.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String tokenA = JsonPath.read(loginResponse, "$.accessToken");

        // Mesmo enviando o X-Tenant-Id do Tenant B, o tenant do JWT (Tenant A) deve
        // prevalecer
        String usersResponse = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + tokenA)
                .header("X-Tenant-Id", tenantB.getId().toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(usersResponse).doesNotContain("vitima@tenant-b-header.com");
        assertThat(usersResponse).contains("admin@tenant-a-header.com");
    }

    @Test
    void passwordHashNeverAppearsInUserResponses() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-no-leak-" + UUID.randomUUID()));
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        User admin = new User(tenant, "admin@no-leak.com", passwordEncoder.encode("senha123"));
        admin.addRole(adminRole);
        userRepository.save(admin);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content("{\"email\":\"admin@no-leak.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(loginResponse, "$.accessToken");

        String usersResponse = mockMvc.perform(get("/api/users")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(usersResponse).doesNotContain("passwordHash");
        assertThat(usersResponse).doesNotContain("$2a$");
        assertThat(usersResponse).doesNotContain("$2b$");
    }

    @Test
    void clientSecretHashNeverAppearsInOAuthClientCreationResponse() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-oauth-no-leak-" + UUID.randomUUID()));
        Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

        User admin = new User(tenant, "admin@oauth-no-leak.com", passwordEncoder.encode("senha123"));
        admin.addRole(adminRole);
        userRepository.save(admin);

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content("{\"email\":\"admin@oauth-no-leak.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(loginResponse, "$.accessToken");

        String createResponse = mockMvc.perform(post("/api/oauth-clients")
                .header("Authorization", "Bearer " + token)
                .contentType("application/json")
                .content("{\"name\":\"Billing Service\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(createResponse).doesNotContain("clientSecretHash");
        assertThat(createResponse).doesNotContain("secretHash");
    }

    @Test
    void mfaSecretEncryptedNeverAppearsInAnyResponse() throws Exception {
        Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-mfa-no-leak-" + UUID.randomUUID()));
        userRepository.save(new User(tenant, "user@mfa-no-leak.com", passwordEncoder.encode("senha123")));

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                .header("X-Tenant-Id", tenant.getId().toString())
                .contentType("application/json")
                .content("{\"email\":\"user@mfa-no-leak.com\",\"password\":\"senha123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = JsonPath.read(loginResponse, "$.accessToken");

        String setupResponse = mockMvc.perform(post("/api/auth/mfa/setup")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(setupResponse).doesNotContain("mfaSecretEncrypted");
        assertThat(setupResponse).doesNotContain("secretEncrypted");
    }

    @Test
    void standardSecurityHeadersArePresentOnEveryResponse() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(result -> assertThat(result.getResponse().getHeader("X-Content-Type-Options"))
                        .isEqualTo("nosniff"))
                .andExpect(result -> assertThat(result.getResponse().getHeader("X-Frame-Options")).isEqualTo("DENY"))
                .andExpect(result -> assertThat(result.getResponse().getHeader("X-XSS-Protection")).isEqualTo("0"));
    }

}