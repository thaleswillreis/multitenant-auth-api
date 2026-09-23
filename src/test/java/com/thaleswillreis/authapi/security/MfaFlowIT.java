package com.thaleswillreis.authapi.security;

import com.jayway.jsonpath.JsonPath;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.model.Role;
import com.thaleswillreis.authapi.repository.RoleRepository;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class MfaFlowIT {

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
        private RoleRepository roleRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private PasswordEncoder passwordEncoder;

        @Test
        void completeMfaEnrollmentAndLoginFlow() throws Exception {
                Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-mfa-it"));
                Role memberRole = roleRepository.findByName("MEMBER").orElseThrow();

                User user = new User(tenant, "joao@acme-mfa-it.com", passwordEncoder.encode("senha123"));
                user.addRole(memberRole);
                userRepository.save(user);

                String loginPayload = "{\"email\":\"joao@acme-mfa-it.com\",\"password\":\"senha123\"}";
                String loginResponse = mockMvc.perform(post("/api/auth/login")
                                .header("X-Tenant-Id", tenant.getId().toString())
                                .contentType("application/json")
                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                String accessToken = JsonPath.read(loginResponse, "$.accessToken");

                String setupResponse = mockMvc.perform(post("/api/auth/mfa/setup")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                String secret = JsonPath.read(setupResponse, "$.secret");
                String qrCodeImage = JsonPath.read(setupResponse, "$.qrCodeImage");
                assertThat(qrCodeImage).startsWith("data:image/png;base64,");

                String enablePayload = "{\"code\":\"" + generateCode(secret) + "\"}";
                mockMvc.perform(post("/api/auth/mfa/enable")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType("application/json")
                                .content(enablePayload))
                                .andExpect(status().isNoContent());

                String secondLoginResponse = mockMvc.perform(post("/api/auth/login")
                                .header("X-Tenant-Id", tenant.getId().toString())
                                .contentType("application/json")
                                .content(loginPayload))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                Boolean mfaRequired = JsonPath.read(secondLoginResponse, "$.mfaRequired");
                assertThat(mfaRequired).isTrue();
                String challengeToken = JsonPath.read(secondLoginResponse, "$.challengeToken");

                String verifyPayload = "{\"challengeToken\":\"" + challengeToken + "\",\"code\":\""
                                + generateCode(secret)
                                + "\"}";
                String verifyResponse = mockMvc.perform(post("/api/auth/verify-mfa")
                                .contentType("application/json")
                                .content(verifyPayload))
                                .andExpect(status().isOk())
                                .andReturn().getResponse().getContentAsString();

                String finalAccessToken = JsonPath.read(verifyResponse, "$.accessToken");

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + finalAccessToken))
                                .andExpect(status().isOk());

                mockMvc.perform(post("/api/auth/verify-mfa")
                                .contentType("application/json")
                                .content(verifyPayload))
                                .andExpect(status().isUnauthorized());
        }

        private String generateCode(String secret) throws Exception {
                long counter = new SystemTimeProvider().getTime() / 30;
                return new DefaultCodeGenerator().generate(secret, counter);
        }

}