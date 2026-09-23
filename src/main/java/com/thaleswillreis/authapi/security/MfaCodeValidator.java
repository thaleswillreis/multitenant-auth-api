package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.model.User;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.springframework.stereotype.Component;

@Component
public class MfaCodeValidator {

    private final MfaEncryptionService encryptionService;
    private final CodeVerifier codeVerifier = new DefaultCodeVerifier(new DefaultCodeGenerator(),
            new SystemTimeProvider());

    public MfaCodeValidator(MfaEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public boolean isValid(User user, String code) {
        if (user.getMfaSecretEncrypted() == null) {
            return false;
        }

        String secret = encryptionService.decrypt(user.getMfaSecretEncrypted());
        return codeVerifier.isValidCode(secret, code);
    }

}