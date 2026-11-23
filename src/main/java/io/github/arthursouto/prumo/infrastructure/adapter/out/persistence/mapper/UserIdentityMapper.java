package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.user.UserIdentity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserIdentityEntity;

public final class UserIdentityMapper {

    private UserIdentityMapper() {
    }

    public static UserIdentityEntity toEntity(UserIdentity identity) {
        UserIdentityEntity entity = new UserIdentityEntity();
        entity.setId(identity.getId());
        entity.setUserId(identity.getUserId());
        entity.setProvider(identity.getProvider());
        entity.setProviderId(identity.getProviderId());
        entity.setCreatedAt(identity.getCreatedAt());
        return entity;
    }

    public static UserIdentity toDomain(UserIdentityEntity entity) {
        return UserIdentity.restore(
                entity.getId(),
                entity.getUserId(),
                entity.getProvider(),
                entity.getProviderId(),
                entity.getCreatedAt()
        );
    }
}
