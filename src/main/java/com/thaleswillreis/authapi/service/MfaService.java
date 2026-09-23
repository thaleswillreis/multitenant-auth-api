package com.thaleswillreis.authapi.service;

import com.thaleswillreis.authapi.dto.MfaCodeRequest;
import com.thaleswillreis.authapi.dto.MfaSetupResponse;
import com.thaleswillreis.authapi.model.SecurityEventType;
import com.thaleswillreis.authapi.model.User;
import com.thaleswillreis.authapi.repository.UserRepository;
import com.thaleswillreis.authapi.security.MfaCodeValidator;
import com.thaleswillreis.authapi.security.MfaEncryptionService;
import com.thaleswillreis.authapi.security.SecurityAuditService;
import com.thaleswillreis.authapi.util.CurrentUserResolver;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.util.Utils;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class MfaService {

    private final UserRepository userRepository;
    private final MfaEncryptionService encryptionService;
    private final MfaCodeValidator mfaCodeValidator;
    private final SecurityAuditService securityAuditService;

    private final SecretGenerator secretGenerator = new DefaultSecretGenerator();
    private final QrGenerator qrGenerator = new ZxingPngQrGenerator();

    public MfaService(UserRepository userRepository, MfaEncryptionService encryptionService,
            MfaCodeValidator mfaCodeValidator, SecurityAuditService securityAuditService) {
        this.userRepository = userRepository;
        this.encryptionService = encryptionService;
        this.mfaCodeValidator = mfaCodeValidator;
        this.securityAuditService = securityAuditService;
    }

    @Transactional
    public MfaSetupResponse setup(String clientIp) {
        User user = currentUser();

        String secret = secretGenerator.generate();
        user.setMfaSecretEncrypted(encryptionService.encrypt(secret));

        QrData data = new QrData.Builder()
                .label(user.getEmail())
                .secret(secret)
                .issuer("multitenant-auth-api")
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build();

        String qrCodeImage;
        try {
            byte[] qrImage = qrGenerator.generate(data);
            qrCodeImage = Utils.getDataUriForImage(qrImage, qrGenerator.getImageMimeType());
        } catch (QrGenerationException ex) {
            throw new IllegalStateException("Falha ao gerar QR Code do MFA", ex);
        }

        return new MfaSetupResponse(secret, data.getUri(), qrCodeImage);
    }

    @Transactional
    public void enable(MfaCodeRequest request, String clientIp) {
        User user = currentUser();

        if (user.getMfaSecretEncrypted() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Nenhuma configuracao de MFA pendente. Chame /api/auth/mfa/setup primeiro.");
        }

        if (!mfaCodeValidator.isValid(user, request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo invalido");
        }

        user.setMfaEnabled(true);
        securityAuditService.record(user.getTenant().getId(), SecurityEventType.MFA_ENABLED, user.getEmail(), true,
                clientIp);
    }

    @Transactional
    public void disable(MfaCodeRequest request, String clientIp) {
        User user = currentUser();

        if (!user.isMfaEnabled() || user.getMfaSecretEncrypted() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "MFA nao esta ativado");
        }

        if (!mfaCodeValidator.isValid(user, request.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Codigo invalido");
        }

        user.setMfaEnabled(false);
        user.setMfaSecretEncrypted(null);
        securityAuditService.record(user.getTenant().getId(), SecurityEventType.MFA_DISABLED, user.getEmail(), true,
                clientIp);
    }

    private User currentUser() {
        UUID userId = CurrentUserResolver.resolve();
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario nao encontrado"));
    }

}