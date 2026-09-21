package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.dto.ClientCredentialsRequest;
import com.thaleswillreis.authapi.dto.ClientTokenResponse;
import com.thaleswillreis.authapi.dto.LoginRequest;
import com.thaleswillreis.authapi.dto.LoginResponse;
import com.thaleswillreis.authapi.dto.RefreshRequest;
import com.thaleswillreis.authapi.model.OAuthClient;
import com.thaleswillreis.authapi.model.SecurityEventType;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.OAuthClientRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import com.thaleswillreis.authapi.security.JwtService;
import com.thaleswillreis.authapi.security.LoginAttemptService;
import com.thaleswillreis.authapi.security.SecurityAuditService;
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
    private static final String INVALID_TOKEN_MESSAGE = "Token invalido ou expirado";
    private static final String BEARER_PREFIX = "Bearer ";

    private final UserRepository userRepository;
    private final OAuthClientRepository oAuthClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final TokenBlacklistService tokenBlacklistService;
    private final LoginAttemptService loginAttemptService;
    private final SecurityAuditService securityAuditService;

    public AuthService(UserRepository userRepository, OAuthClientRepository oAuthClientRepository,
            PasswordEncoder passwordEncoder, JwtService jwtService, JwtProperties jwtProperties,
            TokenBlacklistService tokenBlacklistService, LoginAttemptService loginAttemptService,
            SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.oAuthClientRepository = oAuthClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.tokenBlacklistService = tokenBlacklistService;
        this.loginAttemptService = loginAttemptService;
        this.securityAuditService = securityAuditService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request, String clientIp) {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant nao informado");
        }

        if (loginAttemptService.isLocked(tenantId, request.getEmail())) {
            securityAuditService.record(tenantId, SecurityEventType.ACCOUNT_LOCKED, request.getEmail(), false,
                    clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        User user = userRepository.findByTenantIdAndEmail(tenantId, request.getEmail())
                .orElse(null);

        boolean passwordMatches = user != null
                && passwordEncoder.matches(request.getPassword(), user.getPasswordHash());

        if (user == null || !user.isActive() || !passwordMatches) {
            loginAttemptService.recordFailure(tenantId, request.getEmail());
            securityAuditService.record(tenantId, SecurityEventType.LOGIN_FAILURE, request.getEmail(), false, clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        loginAttemptService.recordSuccess(tenantId, request.getEmail());
        securityAuditService.record(tenantId, SecurityEventType.LOGIN_SUCCESS, request.getEmail(), true, clientIp);

        return issueTokenPair(user);
    }

    public void logout(String authorizationHeader, String clientIp) {
        if (authorizationHeader == null || !authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Header Authorization ausente ou invalido");
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length());
        Claims claims = parseTokenOrThrow(token);

        if (!"access".equals(claims.get("type"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Apenas access tokens podem ser revogados");
        }

        tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());

        UUID tenantId = parseTenantIdOrNull(claims);
        securityAuditService.record(tenantId, SecurityEventType.LOGOUT, claims.get("email", String.class), true,
                clientIp);
    }

    @Transactional(readOnly = true)
    public LoginResponse refresh(RefreshRequest request, String clientIp) {
        Claims claims = parseTokenOrThrow(request.getRefreshToken());

        if (!"refresh".equals(claims.get("type"))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Token informado nao e um refresh token");
        }

        if (claims.getId() != null && tokenBlacklistService.isBlacklisted(claims.getId())) {
            securityAuditService.record(parseTenantIdOrNull(claims), SecurityEventType.TOKEN_REFRESH_FAILURE,
                    claims.get("email", String.class), false, clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token revogado");
        }

        UUID tenantId;
        UUID userId;
        try {
            tenantId = UUID.fromString(claims.get("tenant_id", String.class));
            userId = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
        }

        TenantContext.setCurrentTenant(tenantId);
        try {
            User user = userRepository.findById(userId)
                    .filter(u -> u.getTenant().getId().equals(tenantId))
                    .orElse(null);

            if (user == null || !user.isActive()) {
                securityAuditService.record(tenantId, SecurityEventType.TOKEN_REFRESH_FAILURE,
                        claims.get("email", String.class), false, clientIp);
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
            }

            tokenBlacklistService.blacklist(claims.getId(), claims.getExpiration().toInstant());
            securityAuditService.record(tenantId, SecurityEventType.TOKEN_REFRESH_SUCCESS, user.getEmail(), true,
                    clientIp);

            return issueTokenPair(user);
        } finally {
            TenantContext.clear();
        }
    }

    @Transactional(readOnly = true)
    public ClientTokenResponse clientCredentials(ClientCredentialsRequest request, String clientIp) {
        OAuthClient client = oAuthClientRepository.findByClientId(request.getClientId())
                .orElse(null);

        boolean secretMatches = client != null
                && passwordEncoder.matches(request.getClientSecret(), client.getClientSecretHash());

        if (client == null || !client.isActive() || !secretMatches) {
            UUID tenantId = client != null ? client.getTenant().getId() : null;
            securityAuditService.record(tenantId, SecurityEventType.CLIENT_CREDENTIALS_FAILURE, request.getClientId(),
                    false, clientIp);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_CREDENTIALS_MESSAGE);
        }

        String accessToken = jwtService.generateClientAccessToken(client);
        long expiresInSeconds = jwtProperties.getAccessTokenExpirationMinutes() * 60;

        securityAuditService.record(client.getTenant().getId(), SecurityEventType.CLIENT_CREDENTIALS_SUCCESS,
                client.getClientId(), true, clientIp);

        return new ClientTokenResponse(accessToken, "Bearer", expiresInSeconds);
    }

    private LoginResponse issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        long expiresInSeconds = jwtProperties.getAccessTokenExpirationMinutes() * 60;

        return new LoginResponse(accessToken, refreshToken, "Bearer", expiresInSeconds);
    }

    private Claims parseTokenOrThrow(String token) {
        try {
            return jwtService.parseToken(token);
        } catch (JwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, INVALID_TOKEN_MESSAGE);
        }
    }

    private UUID parseTenantIdOrNull(Claims claims) {
        try {
            return UUID.fromString(claims.get("tenant_id", String.class));
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

}