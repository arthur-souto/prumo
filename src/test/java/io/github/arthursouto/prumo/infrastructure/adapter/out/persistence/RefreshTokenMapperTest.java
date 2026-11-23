package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence;

import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshTokenMapperTest {

    private static final Instant CREATED_AT = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant EXPIRES_AT = Instant.parse("2026-01-08T00:00:00Z");
    private static final Instant REVOKED_AT = Instant.parse("2026-01-02T00:00:00Z");

    @Test
    void toEntityCopiesEveryField() {
        RefreshToken token = RefreshToken.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "hash",
                CREATED_AT,
                EXPIRES_AT,
                REVOKED_AT
        );

        RefreshTokenEntity entity = RefreshTokenMapper.toEntity(token);

        assertThat(entity.getId()).isEqualTo(token.getId());
        assertThat(entity.getUserId()).isEqualTo(token.getUserId());
        assertThat(entity.getFamilyId()).isEqualTo(token.getFamilyId());
        assertThat(entity.getTokenHash()).isEqualTo("hash");
        assertThat(entity.getCreatedAt()).isEqualTo(CREATED_AT);
        assertThat(entity.getExpiresAt()).isEqualTo(EXPIRES_AT);
        assertThat(entity.getRevokedAt()).isEqualTo(REVOKED_AT);
    }

    @Test
    void toDomainCopiesEveryField() {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setFamilyId(UUID.randomUUID());
        entity.setTokenHash("hash");
        entity.setCreatedAt(CREATED_AT);
        entity.setExpiresAt(EXPIRES_AT);
        entity.setRevokedAt(null);

        RefreshToken token = RefreshTokenMapper.toDomain(entity);

        assertThat(token.getId()).isEqualTo(entity.getId());
        assertThat(token.getUserId()).isEqualTo(entity.getUserId());
        assertThat(token.getFamilyId()).isEqualTo(entity.getFamilyId());
        assertThat(token.getTokenHash()).isEqualTo("hash");
        assertThat(token.isRevoked()).isFalse();
    }

    @Test
    void roundTripPreservesTheToken() {
        RefreshToken original = RefreshToken.issue(UUID.randomUUID(), UUID.randomUUID(), "hash", CREATED_AT, EXPIRES_AT);
        original.revoke(REVOKED_AT);

        RefreshToken restored = RefreshTokenMapper.toDomain(RefreshTokenMapper.toEntity(original));

        assertThat(restored).usingRecursiveComparison().isEqualTo(original);
    }
}
