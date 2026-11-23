package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.AuthorizationCodeEntity;

public final class AuthorizationCodeMapper {

    private AuthorizationCodeMapper() {
    }

    public static AuthorizationCodeEntity toEntity(AuthorizationCode code) {
        AuthorizationCodeEntity entity = new AuthorizationCodeEntity();
        entity.setId(code.getId());
        entity.setUserId(code.getUserId());
        entity.setPurpose(code.getPurpose());
        entity.setCodeHash(code.getCodeHash());
        entity.setCreatedAt(code.getCreatedAt());
        entity.setExpiresAt(code.getExpiresAt());
        entity.setUsedAt(code.getUsedAt());
        return entity;
    }

    public static AuthorizationCode toDomain(AuthorizationCodeEntity entity) {
        return AuthorizationCode.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getPurpose(),
                entity.getCodeHash(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                entity.getUsedAt()
        );
    }
}
