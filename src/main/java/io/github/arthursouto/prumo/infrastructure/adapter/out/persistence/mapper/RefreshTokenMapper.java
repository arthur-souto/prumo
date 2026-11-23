package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;

public final class RefreshTokenMapper {

    private RefreshTokenMapper() {
    }

    public static RefreshTokenEntity toEntity(RefreshToken refreshToken) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(refreshToken.getId());
        entity.setUserId(refreshToken.getUserId());
        entity.setFamilyId(refreshToken.getFamilyId());
        entity.setTokenHash(refreshToken.getTokenHash());
        entity.setCreatedAt(refreshToken.getCreatedAt());
        entity.setExpiresAt(refreshToken.getExpiresAt());
        entity.setRevokedAt(refreshToken.getRevokedAt());
        return entity;
    }

    public static RefreshToken toDomain(RefreshTokenEntity entity) {
        return RefreshToken.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getFamilyId(),
                entity.getTokenHash(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt()
        );
    }
}
