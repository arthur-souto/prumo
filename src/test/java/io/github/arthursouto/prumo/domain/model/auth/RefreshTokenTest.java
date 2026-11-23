package io.github.arthursouto.prumo.domain.model.auth;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenTest {

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @Test
    void issueCreatesActiveTokenWithGivenData() {
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        RefreshToken token = RefreshToken.issue(userId, familyId, "hash", NOW, NOW.plusSeconds(60));

        assertThat(token.getId()).isNotNull();
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getFamilyId()).isEqualTo(familyId);
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.getCreatedAt()).isEqualTo(NOW);
        assertThat(token.getExpiresAt()).isEqualTo(NOW.plusSeconds(60));
        assertThat(token.isRevoked()).isFalse();
        assertThat(token.getRevokedAt()).isNull();
    }

    @Test
    void revokeMarksTokenAsRevoked() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(60));

        token.revoke(NOW.plusSeconds(10));

        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void isExpiredIsFalseBeforeExpirationAndTrueFromTheExpirationInstant() {
        RefreshToken token = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", NOW, NOW.plusSeconds(60));

        assertThat(token.isExpired(NOW.plusSeconds(59))).isFalse();
        assertThat(token.isExpired(NOW.plusSeconds(60))).isTrue();
        assertThat(token.isExpired(NOW.plusSeconds(61))).isTrue();
    }

    @Test
    void restoreKeepsEveryValue() {
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID familyId = UUID.randomUUID();

        RefreshToken token = RefreshToken.restore(
                id,
                userId,
                familyId,
                "hash",
                NOW,
                NOW.plusSeconds(60),
                NOW.plusSeconds(5)
        );

        assertThat(token.getId()).isEqualTo(id);
        assertThat(token.getUserId()).isEqualTo(userId);
        assertThat(token.getFamilyId()).isEqualTo(familyId);
        assertThat(token.isRevoked()).isTrue();
        assertThat(token.getRevokedAt()).isEqualTo(NOW.plusSeconds(5));
    }
}
