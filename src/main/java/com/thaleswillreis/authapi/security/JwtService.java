package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.model.OAuthClient;
import com.thaleswillreis.authapi.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService {

    private final RSAPrivateKey privateKey;
    private final RSAPublicKey publicKey;
    private final JwtProperties jwtProperties;

    public JwtService(RSAPrivateKey privateKey, RSAPublicKey publicKey, JwtProperties jwtProperties) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.jwtProperties = jwtProperties;
    }

    public String generateAccessToken(User user) {
        long expirationMillis = jwtProperties.getAccessTokenExpirationMinutes() * 60_000L;
        return buildToken(user, "access", expirationMillis, user.getPermissionNames());
    }

    public String generateRefreshToken(User user) {
        long expirationMillis = jwtProperties.getRefreshTokenExpirationDays() * 24 * 60 * 60_000L;
        // refresh token nao carrega permissoes - nao deve ser usado para autorizar
        // acoes diretamente
        return buildToken(user, "refresh", expirationMillis, Set.of());
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(User user, String type, long expirationMillis, Set<String> permissions) {
        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .subject(user.getId().toString())
                .claim("tenant_id", user.getTenant().getId().toString())
                .claim("email", user.getEmail())
                .claim("type", type)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateClientAccessToken(OAuthClient client) {
        long expirationMillis = jwtProperties.getAccessTokenExpirationMinutes() * 60_000L;
        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .subject(client.getId().toString())
                .claim("tenant_id", client.getTenant().getId().toString())
                .claim("client_id", client.getClientId())
                .claim("type", "client")
                .claim("permissions", client.getPermissionNames())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public String generateMfaChallengeToken(User user) {
        long expirationMillis = jwtProperties.getMfaChallengeExpirationMinutes() * 60_000L;
        Instant now = Instant.now();

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(jwtProperties.getIssuer())
                .subject(user.getId().toString())
                .claim("tenant_id", user.getTenant().getId().toString())
                .claim("type", "mfa_challenge")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

}