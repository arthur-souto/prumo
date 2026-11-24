package io.github.arthursouto.prumo.infrastructure.adapter.out.security;

import io.github.arthursouto.prumo.application.exceptions.GithubFailure;
import io.github.arthursouto.prumo.application.exceptions.GithubIntegrationException;
import io.github.arthursouto.prumo.infrastructure.config.GithubProperties;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AesGcmTokenCipherTest {

    private static String key() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static AesGcmTokenCipher cipher(String key) {
        return new AesGcmTokenCipher(
                new GithubProperties("https://api.github.com", key, new GithubProperties.Polling(true, Duration.ofSeconds(60)))
        );
    }

    @Test
    void roundTripsAToken() {
        AesGcmTokenCipher cipher = cipher(key());

        assertThat(cipher.decrypt(cipher.encrypt("ghp_secret"))).isEqualTo("ghp_secret");
    }

    @Test
    void theCipherTextHidesTheTokenAndDiffersEachTime() {
        AesGcmTokenCipher cipher = cipher(key());

        String first = cipher.encrypt("ghp_secret");
        String second = cipher.encrypt("ghp_secret");

        assertThat(first).doesNotContain("ghp_secret").isNotEqualTo(second);
    }

    @Test
    void aDifferentKeyCannotReadTheToken() {
        String encrypted = cipher(key()).encrypt("ghp_secret");

        assertThatThrownBy(() -> cipher(key()).decrypt(encrypted))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.MISSING_TOKEN));
    }

    @Test
    void tamperedOrGarbageDataIsRejected() {
        AesGcmTokenCipher cipher = cipher(key());
        byte[] bytes = Base64.getDecoder().decode(cipher.encrypt("ghp_secret"));
        bytes[bytes.length - 1] ^= 1;

        assertThatThrownBy(() -> cipher.decrypt(Base64.getEncoder().encodeToString(bytes)))
                .isInstanceOf(GithubIntegrationException.class);
        assertThatThrownBy(() -> cipher.decrypt("not base64 !!")).isInstanceOf(GithubIntegrationException.class);
    }

    @Test
    void withoutAKeyTheCipherReportsTheServerIsNotConfigured() {
        AesGcmTokenCipher cipher = cipher("");

        assertThatThrownBy(() -> cipher.encrypt("x"))
                .isInstanceOfSatisfying(GithubIntegrationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(GithubFailure.NOT_CONFIGURED));
        assertThatThrownBy(() -> cipher.decrypt("x")).isInstanceOf(GithubIntegrationException.class);
    }

    @Test
    void aKeyOfTheWrongSizeFailsFast() {
        assertThatThrownBy(() -> cipher(Base64.getEncoder().encodeToString(new byte[16])))
                .isInstanceOf(IllegalStateException.class);
    }
}
