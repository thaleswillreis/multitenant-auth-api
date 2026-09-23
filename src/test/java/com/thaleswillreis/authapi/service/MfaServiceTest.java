package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.dto.MfaCodeRequest;
import com.thaleswillreis.authapi.dto.MfaSetupResponse;
import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.UserRepository;
import com.thaleswillreis.authapi.security.MfaCodeValidator;
import com.thaleswillreis.authapi.security.MfaEncryptionService;
import com.thaleswillreis.authapi.security.SecurityAuditService;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MfaServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityAuditService securityAuditService;

    private final MfaEncryptionService encryptionService = new MfaEncryptionService(null);
    private final MfaCodeValidator mfaCodeValidator = new MfaCodeValidator(encryptionService);

    private MfaService mfaService;
    private UUID userId;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        mfaService = new MfaService(userRepository, encryptionService, mfaCodeValidator, securityAuditService);

        Tenant tenant = new Tenant("Acme Corp", "acme");
        setField(Tenant.class, tenant, "id", UUID.randomUUID());

        user = new User(tenant, "joao@acme.com", "hashed-password");
        userId = UUID.randomUUID();
        setField(User.class, user, "id", userId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void setField(Class<?> type, Object target, String fieldName, Object value) throws Exception {
        var field = type.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void setupGeneratesSecretAndQrCode() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MfaSetupResponse response = mfaService.setup("127.0.0.1");

        assertThat(response.getSecret()).isNotBlank();
        assertThat(response.getOtpAuthUri()).startsWith("otpauth://totp/");
        assertThat(response.getQrCodeImage()).startsWith("data:image/png;base64,");
        assertThat(user.getMfaSecretEncrypted()).isNotBlank();
        assertThat(user.isMfaEnabled()).isFalse();
    }

    @Test
    void enableActivatesMfaWithValidCode() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MfaSetupResponse setupResponse = mfaService.setup("127.0.0.1");
        String validCode = generateValidCode(setupResponse.getSecret());

        MfaCodeRequest request = new MfaCodeRequest();
        request.setCode(validCode);

        mfaService.enable(request, "127.0.0.1");

        assertThat(user.isMfaEnabled()).isTrue();
    }

    @Test
    void enableRejectsInvalidCode() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        mfaService.setup("127.0.0.1");

        MfaCodeRequest request = new MfaCodeRequest();
        request.setCode("000000");

        assertThatThrownBy(() -> mfaService.enable(request, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Codigo invalido");

        assertThat(user.isMfaEnabled()).isFalse();
    }

    @Test
    void enableFailsWithoutPendingSetup() {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MfaCodeRequest request = new MfaCodeRequest();
        request.setCode("123456");

        assertThatThrownBy(() -> mfaService.enable(request, "127.0.0.1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Nenhuma configuracao");
    }

    @Test
    void disableDeactivatesMfaWithValidCode() throws Exception {
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        MfaSetupResponse setupResponse = mfaService.setup("127.0.0.1");
        String validCode = generateValidCode(setupResponse.getSecret());

        MfaCodeRequest enableRequest = new MfaCodeRequest();
        enableRequest.setCode(validCode);
        mfaService.enable(enableRequest, "127.0.0.1");

        MfaCodeRequest disableRequest = new MfaCodeRequest();
        disableRequest.setCode(generateValidCode(setupResponse.getSecret()));

        mfaService.disable(disableRequest, "127.0.0.1");

        assertThat(user.isMfaEnabled()).isFalse();
        assertThat(user.getMfaSecretEncrypted()).isNull();
    }

    private String generateValidCode(String secret) throws Exception {
        long counter = new SystemTimeProvider().getTime() / 30;
        return new DefaultCodeGenerator().generate(secret, counter);
    }

}