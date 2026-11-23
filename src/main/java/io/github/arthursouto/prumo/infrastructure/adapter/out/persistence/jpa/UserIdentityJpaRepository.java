package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityJpaRepository extends JpaRepository<UserIdentityEntity, UUID> {

    Optional<UserIdentityEntity> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<UserIdentityEntity> findByUserIdAndProvider(UUID userId, AuthProvider provider);

    List<UserIdentityEntity> findByUserId(UUID userId);
}
