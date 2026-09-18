package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.dto.LoginRequest;
import com.thaleswillreis.authapi.dto.LoginResponse;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.UserRepository;
import com.thaleswillreis.authapi.security.JwtService;
import com.thaleswillreis.authapi.security.TokenBlacklistService;
import com.thaleswillreis.authapi.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE = "Credenciais invalidas";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                        JwtService jwtService, JwtProperties jwtProperties,
                        TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao informado");
        }

        User user = userRepository.findByTenantIdAndEmail(tenantId, request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE));

        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (!user.isActive() || !passwordMatches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresInSeconds = jwtProperties.getAccessTokenExpirationMinutes() * 60;

        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header Authorization ausente ou invalido");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());

        Claims claims;
        try {
            claims = jwtService.parseToken(token);
        } catch (JwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token invalido ou expirado");
        }

        if (!"access".equals(claims.get("type"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apenas access tokens podem ser revogados");
        }

        tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());
    }

}