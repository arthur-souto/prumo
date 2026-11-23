package io.github.arthursouto.prumo.domain.model.auth;

import org.junit.jupiter.api.Test;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCodePurpose;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuthorizationCodeTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void issueCreatesAnUnusedCode() {
        UUID userId = UUID.randomUUID();

        AuthorizationCode code = AuthorizationCode.issue(userId, AuthorizationCodePurpose.LOGIN, "hash", NOW, NOW.plusSeconds(60));

        assertThat(code.getId()).isNotNull();
        assertThat(code.getUserId()).isEqualTo(userId);
        assertThat(code.getCodeHash()).isEqualTo("hash");
        assertThat(code.getCreatedAt()).isEqualTo(NOW);
        assertThat(code.getExpiresAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(code.isUsed()).isFalse();
        assertThat(code.getUsedAt()).isNull();
    }

    @Test
    void isExpiredIsFalseBeforeExpirationAndTrueFromTheExpirationInstant() {
        AuthorizationCode code = AuthorizationCode.issue(UUID.randomUUID(), AuthorizationCodePurpose.LOGIN, "hash", NOW, NOW.plusSeconds(60));

        assertThat(code.isExpired(NOW.plusSeconds(59))).isFalse();
        assertThat(code.isExpired(NOW.plusSeconds(60))).isTrue();
    }

    @Test
    void restoreKeepsTheUsedState() {
        AuthorizationCode code = AuthorizationCode.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                AuthorizationCodePurpose.LOGIN, "hash",
                NOW,
                NOW.plusSeconds(60),
                NOW.plusSeconds(5)
        );

        assertThat(code.isUsed()).isTrue();
        assertThat(code.getUsedAt()).isEqualTo(NOW.plusSeconds(5));
    }
}
