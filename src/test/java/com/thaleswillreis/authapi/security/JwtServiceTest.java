package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        JwtProperties properties = new JwtProperties();
        properties.setIssuer("test-issuer");
        properties.setAccessTokenExpirationMinutes(15);
        properties.setRefreshTokenExpirationDays(7);

        jwtService = new JwtService(
                (RSAPrivateKey) keyPair.getPrivate(),
                (RSAPublicKey) keyPair.getPublic(),
                properties
        );

        user = newUser(UUID.randomUUID(), UUID.randomUUID(), "joao@acme.com");
    }

    @Test
    void accessTokenContainsExpectedClaims() {
        String token = jwtService.generateAccessToken(user);

        Claims claims = jwtService.parseToken(token);

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get("tenant_id")).isEqualTo(user.getTenant().getId().toString());
        assertThat(claims.get("email")).isEqualTo("joao@acme.com");
        assertThat(claims.get("type")).isEqualTo("access");
    }

    @Test
    void refreshTokenHasLaterExpirationThanAccessToken() {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        Claims accessClaims = jwtService.parseToken(accessToken);
        Claims refreshClaims = jwtService.parseToken(refreshToken);

        assertThat(refreshClaims.get("type")).isEqualTo("refresh");
        assertThat(refreshClaims.getExpiration()).isAfter(accessClaims.getExpiration());
    }

    private User newUser(UUID tenantId, UUID userId, String email) throws Exception {
        Tenant tenant = new Tenant("Acme Corp", "acme");
        setId(Tenant.class, tenant, tenantId);

        User user = new User(tenant, email, "hashed-password");
        setId(User.class, user, userId);
        return user;
    }

    private void setId(Class<?> type, Object target, UUID id) throws Exception {
        var idField = type.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }

}