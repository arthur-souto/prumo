package io.github.arthursouto.prumo.infrastructure.adapter.out.security;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.application.port.out.TokenCipherPort;
import io.github.arthursouto.prumo.infrastructure.config.GithubProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class AesGcmTokenCipher implements TokenCipherPort {

    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final SecureRandom random = new SecureRandom();
    private final byte[] key;

    public AesGcmTokenCipher(GithubProperties properties) {
        this.key = decodeKey(properties.encryptionKey());
    }

    @Override
    public String encrypt(String plain) {
        requireKey();

        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array()
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Could not encrypt the token", exception);
        }
    }

    @Override
    public String decrypt(String cipherText) {
        requireKey();

        try {
            ByteBuffer buffer = ByteBuffer.wrap(Base64.getDecoder().decode(cipherText));
            byte[] iv = new byte[IV_BYTES];
            buffer.get(iv);
            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));

            return new String(cipher.doFinal(encrypted), java.nio.charset.StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new GithubIntegrationException(
                    GithubFailure.MISSING_TOKEN,
                    "The stored GitHub token cannot be read. Save it again"
            );
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new GithubIntegrationException(
                    GithubFailure.NOT_CONFIGURED,
                    "The server has no encryption key configured for GitHub tokens"
            );
        }
    }

    private static byte[] decodeKey(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return null;
        }

        byte[] decoded = Base64.getDecoder().decode(encoded.strip());

        if (decoded.length != KEY_BYTES) {
            throw new IllegalStateException("app.github.encryption-key must be 32 bytes encoded in Base64");
        }

        return decoded;
    }
}
