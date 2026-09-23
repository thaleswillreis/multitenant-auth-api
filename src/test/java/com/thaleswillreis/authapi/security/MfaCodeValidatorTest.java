package com.thaleswillreis.authapi.security;

import com.thaleswillreis.authapi.model.Tenant;
import com.thaleswillreis.authapi.model.User;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MfaCodeValidatorTest {

    private final MfaEncryptionService encryptionService = new MfaEncryptionService(null);
    private final MfaCodeValidator validator = new MfaCodeValidator(encryptionService);

    @Test
    void validatesCorrectCode() throws Exception {
        String secret = new DefaultSecretGenerator().generate();
        User user = new User(new Tenant("Acme Corp", "acme"), "joao@acme.com", "hash");
        user.setMfaSecretEncrypted(encryptionService.encrypt(secret));

        long counter = new SystemTimeProvider().getTime() / 30;
        String code = new DefaultCodeGenerator().generate(secret, counter);

        assertThat(validator.isValid(user, code)).isTrue();
    }

    @Test
    void rejectsIncorrectCode() {
        String secret = new DefaultSecretGenerator().generate();
        User user = new User(new Tenant("Acme Corp", "acme"), "joao@acme.com", "hash");
        user.setMfaSecretEncrypted(encryptionService.encrypt(secret));

        assertThat(validator.isValid(user, "000000")).isFalse();
    }

    @Test
    void rejectsWhenNoSecretConfigured() {
        User user = new User(new Tenant("Acme Corp", "acme"), "joao@acme.com", "hash");

        assertThat(validator.isValid(user, "123456")).isFalse();
    }

}