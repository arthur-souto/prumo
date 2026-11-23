package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserIdentityRepositoryPort {

    UserIdentity save(UserIdentity identity);

    Optional<UserIdentity> findByProviderAndProviderId(AuthProvider provider, String providerId);

    Optional<UserIdentity> findByUserIdAndProvider(UUID userId, AuthProvider provider);

    List<UserIdentity> findByUserId(UUID userId);

    void delete(UUID identityId);
}
