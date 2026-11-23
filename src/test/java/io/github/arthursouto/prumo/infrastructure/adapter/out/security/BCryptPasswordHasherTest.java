package io.github.arthursouto.prumo.infrastructure.adapter.out.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void hashDoesNotExposeRawPassword() {
        String hash = hasher.hash("s3cret-pass");

        assertThat(hash).isNotEqualTo("s3cret-pass").startsWith("$2");
    }

    @Test
    void hashIsSaltedSoEqualPasswordsProduceDifferentHashes() {
        assertThat(hasher.hash("s3cret-pass")).isNotEqualTo(hasher.hash("s3cret-pass"));
    }

    @Test
    void matchesAcceptsTheOriginalPassword() {
        String hash = hasher.hash("s3cret-pass");

        assertThat(hasher.matches("s3cret-pass", hash)).isTrue();
    }

    @Test
    void matchesRejectsAnotherPassword() {
        String hash = hasher.hash("s3cret-pass");

        assertThat(hasher.matches("other-pass", hash)).isFalse();
    }
}
