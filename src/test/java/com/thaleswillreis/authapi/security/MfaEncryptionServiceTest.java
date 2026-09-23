package com.thaleswillreis.authapi.security;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MfaEncryptionServiceTest {

    @Test
    void encryptsAndDecryptsRoundTrip() {
        MfaEncryptionService service = new MfaEncryptionService(null);

        String secret = "BP26TDZUZ5SVPZJRIHCAUVREO5EWMHHV";
        String encrypted = service.encrypt(secret);

        assertThat(encrypted).isNotEqualTo(secret);
        assertThat(service.decrypt(encrypted)).isEqualTo(secret);
    }

    @Test
    void producesDifferentCiphertextForSameInput() {
        MfaEncryptionService service = new MfaEncryptionService(null);

        String secret = "BP26TDZUZ5SVPZJRIHCAUVREO5EWMHHV";
        String encryptedOnce = service.encrypt(secret);
        String encryptedAgain = service.encrypt(secret);

        assertThat(encryptedOnce).isNotEqualTo(encryptedAgain);
    }

    @Test
    void rejectsExplicitlyConfiguredKeyWithWrongLength() {
        String tooShortKey = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> new MfaEncryptionService(tooShortKey))
                .isInstanceOf(IllegalStateException.class);
    }

}