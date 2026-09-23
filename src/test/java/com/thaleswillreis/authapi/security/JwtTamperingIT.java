package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.model.Role;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.RoleRepository;
import com.thaleswillreis.authapi.repository.TenantRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class JwtTamperingIT {

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

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RSAPrivateKey jwtPrivateKey;

    private Tenant tenant;
    private User user;
    private String validAccessToken;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(new Tenant("Acme Corp", "acme-jwt-tampering-" + UUID.randomUUID()));
        Role memberRole = roleRepository.findByName("MEMBER").orElseThrow();

        user = new User(tenant, "user@jwt-tampering.com", passwordEncoder.encode("senha123"));
        user.addRole(memberRole);
        userRepository.save(user);

        validAccessToken = jwtService.generateAccessToken(user);
    }

    @Test
    void rejectsTokenWithNoneAlgorithm() throws Exception {
        String header = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payloadJson = "{\"sub\":\"" + user.getId() + "\",\"tenant_id\":\"" + tenant.getId()
                + "\",\"type\":\"access\",\"permissions\":[\"USER_READ\",\"USER_WRITE\",\"USER_DELETE\"]}";
        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payloadJson.getBytes(StandardCharsets.UTF_8));
        String forgedToken = header + "." + payload + ".";

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithTamperedPayload() throws Exception {
        String[] parts = validAccessToken.split("\\.");
        String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String tamperedJson = payloadJson.replace(tenant.getId().toString(), UUID.randomUUID().toString());
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(tamperedJson.getBytes(StandardCharsets.UTF_8));
        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenSignedWithDifferentKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair attackerKeyPair = generator.generateKeyPair();

        String forgedToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("tenant_id", tenant.getId().toString())
                .claim("type", "access")
                .claim("permissions", List.of("USER_READ", "USER_WRITE", "USER_DELETE"))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 900_000))
                .signWith((RSAPrivateKey) attackerKeyPair.getPrivate(), Jwts.SIG.RS256)
                .compact();

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedToken() throws Exception {
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer isso-nao-e-um-jwt-valido"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredToken() throws Exception {
        String expiredToken = Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .claim("tenant_id", tenant.getId().toString())
                .claim("type", "access")
                .claim("permissions", List.of("USER_READ"))
                .issuedAt(new Date(System.currentTimeMillis() - 1_000_000))
                .expiration(new Date(System.currentTimeMillis() - 500_000))
                .signWith(jwtPrivateKey, Jwts.SIG.RS256)
                .compact();

        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

}