package com.thaleswillreis.authapi.security;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;

import com.thaleswillreis.authapi.model.Role;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.PermissionRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import io.jsonwebtoken.Jwts;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class JwtAuthenticationFilterIT {

        @Container
        @ServiceConnection
        static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"));

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private TenantRepository tenantRepository;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private RSAPrivateKey jwtPrivateKey;

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
                User user = userRepository
                                .save(new User(tenant, "joao@acme-jwt-it.com", passwordEncoder.encode("senha123")));

                String refreshToken = jwtService.generateRefreshToken(user);

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + refreshToken))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void allowsAccessAndScopesToTenantFromToken() throws Exception {
                Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-scope-it"));
                Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-scope-it"));

                Role memberRole = roleRepository.findByName("MEMBER").orElseThrow();

                User userA = new User(tenantA, "joao@acme-scope-it.com", passwordEncoder.encode("senha123"));
                userA.addRole(memberRole);
                userRepository.save(userA);
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

                Role memberRole = roleRepository.findByName("MEMBER").orElseThrow();

                User userA = new User(tenantA, "joao@acme-spoof-it.com", passwordEncoder.encode("senha123"));
                userA.addRole(memberRole);
                userRepository.save(userA);
                userRepository.save(new User(tenantB, "maria@beta-spoof-it.com", passwordEncoder.encode("senha123")));

                String accessToken = jwtService.generateAccessToken(userA);

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + accessToken)
                                .header("X-Tenant-Id", tenantB.getId().toString()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(1))
                                .andExpect(jsonPath("$[0].email").value("joao@acme-spoof-it.com"));
        }

        @Test
        void memberCanListButCannotCreateUsers() throws Exception {
                Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-member-it"));
                Role memberRole = roleRepository.findByName("MEMBER").orElseThrow();

                User member = new User(tenant, "membro@acme-member-it.com", passwordEncoder.encode("senha123"));
                member.addRole(memberRole);
                userRepository.save(member);

                String accessToken = jwtService.generateAccessToken(member);

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk());

                String newUserPayload = """
                                {"email":"outro@acme-member-it.com","password":"12345678"}
                                """;

                mockMvc.perform(post("/api/users")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType("application/json")
                                .content(newUserPayload))
                                .andExpect(status().isForbidden());
        }

        @Test
        void adminCanCreateUsers() throws Exception {
                Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-admin-it"));
                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

                User admin = new User(tenant, "admin@acme-admin-it.com", passwordEncoder.encode("senha123"));
                admin.addRole(adminRole);
                userRepository.save(admin);

                String accessToken = jwtService.generateAccessToken(admin);

                String newUserPayload = """
                                {"email":"novo@acme-admin-it.com","password":"12345678"}
                                """;

                mockMvc.perform(post("/api/users")
                                .header("Authorization", "Bearer " + accessToken)
                                .contentType("application/json")
                                .content(newUserPayload))
                                .andExpect(status().isCreated());
        }

        @Test
        void adminCannotFetchUserFromAnotherTenantById() throws Exception {
                Tenant tenantA = tenantRepository.save(new Tenant("Acme Corp", "acme-cross-tenant-it"));
                Tenant tenantB = tenantRepository.save(new Tenant("Beta Corp", "beta-cross-tenant-it"));

                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

                User adminA = new User(tenantA, "admin@acme-cross-tenant-it.com", passwordEncoder.encode("senha123"));
                adminA.addRole(adminRole);
                userRepository.save(adminA);

                User userB = userRepository.save(new User(tenantB, "membro@beta-cross-tenant-it.com",
                                passwordEncoder.encode("senha123")));

                String accessToken = jwtService.generateAccessToken(adminA);

                mockMvc.perform(get("/api/users/" + userB.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isNotFound());
        }

        @Test
        void adminCanFetchUserFromOwnTenantById() throws Exception {
                Tenant tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-same-tenant-it"));
                Role adminRole = roleRepository.findByName("ADMIN").orElseThrow();

                User admin = new User(tenant, "admin@acme-same-tenant-it.com", passwordEncoder.encode("senha123"));
                admin.addRole(adminRole);
                userRepository.save(admin);

                User colleague = userRepository.save(
                                new User(tenant, "colega@acme-same-tenant-it.com", passwordEncoder.encode("senha123")));

                String accessToken = jwtService.generateAccessToken(admin);

                mockMvc.perform(get("/api/users/" + colleague.getId())
                                .header("Authorization", "Bearer " + accessToken))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("colega@acme-same-tenant-it.com"));
        }

        @Test
        void rejectsTokenMissingTenantIdClaim() throws Exception {
                String tokenWithoutTenant = Jwts.builder()
                                .issuer("multitenant-auth-api")
                                .subject(java.util.UUID.randomUUID().toString())
                                .claim("email", "sem-tenant@acme.com")
                                .claim("type", "access")
                                .claim("permissions", java.util.List.of("USER_READ"))
                                .issuedAt(Date.from(Instant.now()))
                                .expiration(Date.from(Instant.now().plusSeconds(900)))
                                .signWith(jwtPrivateKey, Jwts.SIG.RS256)
                                .compact();

                mockMvc.perform(get("/api/users")
                                .header("Authorization", "Bearer " + tokenWithoutTenant))
                                .andExpect(status().isUnauthorized());
        }

}