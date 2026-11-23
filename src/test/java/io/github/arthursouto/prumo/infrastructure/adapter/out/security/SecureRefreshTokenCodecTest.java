package io.github.arthursouto.prumo.infrastructure.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecureRefreshTokenCodecTest {

    private final SecureRefreshTokenCodec codec = new SecureRefreshTokenCodec();

    @Test
    void generatesUniqueUrlSafeTokensWithEnoughEntropy() {
        String first = codec.generate();
        String second = codec.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    void hashIsDeterministicSha256InHex() {
        assertThat(codec.hash("token")).isEqualTo(codec.hash("token")).matches("[0-9a-f]{64}");
        assertThat(codec.hash("token")).isEqualTo("3c469e9d6c5875d37a43f353d4f88e61fcf812c66eee3457465a40b0da4153e0");
    }

    @Test
    void differentTokensProduceDifferentHashes() {
        assertThat(codec.hash("token-a")).isNotEqualTo(codec.hash("token-b"));
    }

    @Test
    void hashDoesNotContainTheRawToken() {
        String raw = codec.generate();

        assertThat(codec.hash(raw)).doesNotContain(raw);
    }
}
