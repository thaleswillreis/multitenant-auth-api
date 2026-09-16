package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;

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
        return buildToken(user, "access", expirationMillis);
    }

    public String generateRefreshToken(User user) {
        long expirationMillis = jwtProperties.getRefreshTokenExpirationDays() * 24 * 60 * 60_000L;
        return buildToken(user, "refresh", expirationMillis);
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private String buildToken(User user, String type, long expirationMillis) {
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(jwtProperties.getIssuer())
                .subject(user.getId().toString())
                .claim("tenant_id", user.getTenant().getId().toString())
                .claim("email", user.getEmail())
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

}