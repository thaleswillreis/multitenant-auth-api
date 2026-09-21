package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.config.JwtProperties;
import com.thaleswillreis.authapi.dto.ClientCredentialsRequest;
import com.thaleswillreis.authapi.dto.LoginRequest;
import com.thaleswillreis.authapi.dto.LoginResponse;
import com.thaleswillreis.authapi.model.OAuthClient;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.dto.RefreshRequest;
import com.thaleswillreis.authapi.repository.OAuthClientRepository;
import com.thaleswillreis.authapi.repository.UserRepository;
import com.thaleswillreis.authapi.security.LoginAttemptService;
import com.thaleswillreis.authapi.security.JwtService;
import com.thaleswillreis.authapi.security.SecurityAuditService;
import com.thaleswillreis.authapi.security.TokenBlacklistService;
import com.thaleswillreis.authapi.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String TEST_IP = "127.0.0.1";

    @Mock
    private OAuthClientRepository oAuthClientRepository;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @Mock
    private LoginAttemptService loginAttemptService;

    @Mock
    private SecurityAuditService securityAuditService;

    @Mock
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private JwtService jwtService;
    private AuthService authService;
    private UUID tenantId;

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
                properties);

        authService = new AuthService(userRepository, oAuthClientRepository, passwordEncoder, jwtService, properties,
                tokenBlacklistService, loginAttemptService, securityAuditService);

        tenantId = UUID.randomUUID();
        TenantContext.setCurrentTenant(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void loginSucceedsWithValidCredentials() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "correct-password", true);

        when(userRepository.findByTenantIdAndEmail(tenantId, "joao@acme.com")).thenReturn(Optional.of(user));
        when(loginAttemptService.isLocked(tenantId, "joao@acme.com")).thenReturn(false);

        LoginResponse response = authService.login(loginRequest("joao@acme.com", "correct-password"), TEST_IP);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void loginFailsWithWrongPassword() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "correct-password", true);

        when(userRepository.findByTenantIdAndEmail(tenantId, "joao@acme.com")).thenReturn(Optional.of(user));
        when(loginAttemptService.isLocked(tenantId, "joao@acme.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("joao@acme.com", "wrong-password"), TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void loginFailsWhenUserDoesNotExist() {
        when(userRepository.findByTenantIdAndEmail(tenantId, "ninguem@acme.com")).thenReturn(Optional.empty());
        when(loginAttemptService.isLocked(tenantId, "ninguem@acme.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("ninguem@acme.com", "qualquer-senha"), TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void loginFailsWhenUserIsInactive() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "correct-password", false);

        when(userRepository.findByTenantIdAndEmail(tenantId, "joao@acme.com")).thenReturn(Optional.of(user));
        when(loginAttemptService.isLocked(tenantId, "joao@acme.com")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(loginRequest("joao@acme.com", "correct-password"), TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    private LoginRequest loginRequest(String email, String password) {
        LoginRequest request = new LoginRequest();
        request.setEmail(email);
        request.setPassword(password);
        return request;
    }

    private User newUser(UUID tenantId, String email, String rawPassword, boolean active) throws Exception {
        Tenant tenant = new Tenant("Acme Corp", "acme");
        setId(Tenant.class, tenant, tenantId);

        User user = new User(tenant, email, passwordEncoder.encode(rawPassword));
        setId(User.class, user, UUID.randomUUID());
        user.setActive(active);
        return user;
    }

    private void setId(Class<?> type, Object target, UUID id) throws Exception {
        var idField = type.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }

    @Test
    void logoutBlacklistsAccessTokenJti() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "senha123", true);
        String accessToken = jwtService.generateAccessToken(user);

        authService.logout("Bearer " + accessToken, TEST_IP);

        ArgumentCaptor<String> jtiCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Instant> expirationCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(tokenBlacklistService).blacklist(jtiCaptor.capture(), expirationCaptor.capture());

        assertThat(jtiCaptor.getValue()).isNotBlank();
        assertThat(expirationCaptor.getValue()).isAfter(Instant.now());
    }

    @Test
    void logoutRejectsRefreshToken() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "senha123", true);
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThatThrownBy(() -> authService.logout("Bearer " + refreshToken, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Apenas access tokens");
    }

    @Test
    void logoutRejectsMissingAuthorizationHeader() {
        assertThatThrownBy(() -> authService.logout(null, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Authorization ausente");
    }

    @Test
    void refreshRotatesTokensAndBlacklistsOldRefreshToken() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "senha123", true);
        String oldRefreshToken = jwtService.generateRefreshToken(user);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(oldRefreshToken);

        LoginResponse response = authService.refresh(request, TEST_IP);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotEqualTo(oldRefreshToken);

        verify(tokenBlacklistService).blacklist(any(), any());
    }

    @Test
    void refreshRejectsAccessTokenAsInput() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "senha123", true);
        String accessToken = jwtService.generateAccessToken(user);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(accessToken);

        assertThatThrownBy(() -> authService.refresh(request, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("nao e um refresh token");
    }

    @Test
    void refreshRejectsAlreadyBlacklistedToken() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "senha123", true);
        String refreshToken = jwtService.generateRefreshToken(user);

        when(tokenBlacklistService.isBlacklisted(any())).thenReturn(true);

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("revogado");
    }

    @Test
    void refreshRejectsInactiveUser() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "senha123", false);
        String refreshToken = jwtService.generateRefreshToken(user);

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        RefreshRequest request = new RefreshRequest();
        request.setRefreshToken(refreshToken);

        assertThatThrownBy(() -> authService.refresh(request, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void clientCredentialsSucceedsWithValidSecret() throws Exception {
        OAuthClient client = newOAuthClient(tenantId, "billing-service", "correct-secret");

        when(oAuthClientRepository.findByClientId("billing-service")).thenReturn(Optional.of(client));

        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientId("billing-service");
        request.setClientSecret("correct-secret");

        var response = authService.clientCredentials(request, TEST_IP);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    void clientCredentialsFailsWithWrongSecret() throws Exception {
        OAuthClient client = newOAuthClient(tenantId, "billing-service", "correct-secret");

        when(oAuthClientRepository.findByClientId("billing-service")).thenReturn(Optional.of(client));

        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientId("billing-service");
        request.setClientSecret("wrong-secret");

        assertThatThrownBy(() -> authService.clientCredentials(request, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    @Test
    void clientCredentialsFailsWhenClientNotFound() {
        when(oAuthClientRepository.findByClientId("nao-existe")).thenReturn(Optional.empty());

        ClientCredentialsRequest request = new ClientCredentialsRequest();
        request.setClientId("nao-existe");
        request.setClientSecret("qualquer-coisa");

        assertThatThrownBy(() -> authService.clientCredentials(request, TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");
    }

    private OAuthClient newOAuthClient(UUID tenantId, String clientId, String rawSecret) throws Exception {
        Tenant tenant = new Tenant("Acme Corp", "acme");
        setId(Tenant.class, tenant, tenantId);

        OAuthClient client = new OAuthClient(tenant, "Test Client", clientId, passwordEncoder.encode(rawSecret));
        setId(OAuthClient.class, client, UUID.randomUUID());
        return client;
    }

    @Test
    void loginFailsImmediatelyWhenAccountIsLocked() {
        when(loginAttemptService.isLocked(tenantId, "joao@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(loginRequest("joao@acme.com", "qualquer-senha"), TEST_IP))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Credenciais invalidas");

        verifyNoInteractions(userRepository);
    }

    @Test
    void loginRecordsFailureOnWrongPassword() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "correct-password", true);

        when(loginAttemptService.isLocked(tenantId, "joao@acme.com")).thenReturn(false);
        when(userRepository.findByTenantIdAndEmail(tenantId, "joao@acme.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(loginRequest("joao@acme.com", "wrong-password"), TEST_IP))
                .isInstanceOf(ResponseStatusException.class);

        verify(loginAttemptService).recordFailure(tenantId, "joao@acme.com");
    }

    @Test
    void loginRecordsSuccessOnCorrectPassword() throws Exception {
        User user = newUser(tenantId, "joao@acme.com", "correct-password", true);

        when(loginAttemptService.isLocked(tenantId, "joao@acme.com")).thenReturn(false);
        when(userRepository.findByTenantIdAndEmail(tenantId, "joao@acme.com")).thenReturn(Optional.of(user));

        authService.login(loginRequest("joao@acme.com", "correct-password"), TEST_IP);

        verify(loginAttemptService).recordSuccess(tenantId, "joao@acme.com");
    }

}