package com.thaleswillreis.authapi.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class MfaEncryptionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MfaEncryptionService.class);
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int KEY_LENGTH_BYTES = 32;

    private final SecretKeySpec keySpec;

    public MfaEncryptionService(@Value("${app.mfa.encryption-key:}") String configuredKey) {
        byte[] keyBytes;

        if (configuredKey == null || configuredKey.isBlank()) {
            LOGGER.error("app.mfa.encryption-key NAO configurada - usando chave de desenvolvimento INSEGURA. "
                    + "Gere uma chave real com `openssl rand -base64 32` e defina MFA_ENCRYPTION_KEY no .env "
                    + "antes de usar este projeto em qualquer ambiente que nao seja teste local.");
            keyBytes = deriveDevOnlyKey();
        } else {
            keyBytes = Base64.getDecoder().decode(configuredKey);
            if (keyBytes.length != KEY_LENGTH_BYTES) {
                throw new IllegalStateException(
                        "app.mfa.encryption-key deve decodificar para exatamente 32 bytes (AES-256). "
                                + "Gere uma nova com: openssl rand -base64 32");
            }
        }

        this.keySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherText.length);
            buffer.put(iv);
            buffer.put(cipherText);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao criptografar segredo MFA", ex);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] data = Base64.getDecoder().decode(encoded);
            byte[] iv = Arrays.copyOfRange(data, 0, GCM_IV_LENGTH_BYTES);
            byte[] cipherText = Arrays.copyOfRange(data, GCM_IV_LENGTH_BYTES, data.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));

            byte[] plainBytes = cipher.doFinal(cipherText);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Falha ao descriptografar segredo MFA", ex);
        }
    }

    private byte[] deriveDevOnlyKey() {
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest("multitenant-auth-api-dev-only-mfa-key".getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

}