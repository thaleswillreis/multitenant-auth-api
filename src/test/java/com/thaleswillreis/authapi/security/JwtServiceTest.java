package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.model.Permission;
import com.thaleswillreis.authapi.model.Role;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
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

        user = newUser(UUID.randomUUID(), "joao@acme.com");
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

    @Test
    @SuppressWarnings("unchecked")
    void accessTokenIncludesPermissionsFromAssignedRoles() throws Exception {
        Permission userWrite = newPermission(UUID.randomUUID(), "USER_WRITE");
        Role adminRole = newRole(UUID.randomUUID(), "ADMIN", Set.of(userWrite));
        user.addRole(adminRole);

        String token = jwtService.generateAccessToken(user);
        Claims claims = jwtService.parseToken(token);

        List<String> permissions = claims.get("permissions", List.class);
        assertThat(permissions).containsExactly("USER_WRITE");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshTokenHasNoPermissions() throws Exception {
        Permission userWrite = newPermission(UUID.randomUUID(), "USER_WRITE");
        Role adminRole = newRole(UUID.randomUUID(), "ADMIN", Set.of(userWrite));
        user.addRole(adminRole);

        String token = jwtService.generateRefreshToken(user);
        Claims claims = jwtService.parseToken(token);

        List<String> permissions = claims.get("permissions", List.class);
        assertThat(permissions).isEmpty();
    }

    private User newUser(UUID tenantId, String email) throws Exception {
        Tenant tenant = new Tenant("Acme Corp", "acme");
        setField(Tenant.class, tenant, "id", tenantId);

        User user = new User(tenant, email, "hashed-password");
        setField(User.class, user, "id", UUID.randomUUID());
        return user;
    }

    private Permission newPermission(UUID id, String name) throws Exception {
        var constructor = Permission.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Permission permission = constructor.newInstance();
        setField(Permission.class, permission, "id", id);
        setField(Permission.class, permission, "name", name);
        return permission;
    }

    private Role newRole(UUID id, String name, java.util.Set<Permission> permissions) throws Exception {
        var constructor = Role.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Role role = constructor.newInstance();
        setField(Role.class, role, "id", id);
        setField(Role.class, role, "name", name);
        role.getPermissions().addAll(permissions);
        return role;
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value) throws Exception {
        var field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

}