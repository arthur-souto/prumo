package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.UserIdentityRepositoryPort;
import io.github.arthursouto.prumo.domain.model.user.AuthProvider;
import io.github.arthursouto.prumo.domain.model.user.UserIdentity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.UserIdentityJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.UserIdentityMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserIdentityPersistenceAdapter implements UserIdentityRepositoryPort {

    private final UserIdentityJpaRepository repository;

    public UserIdentityPersistenceAdapter(UserIdentityJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserIdentity save(UserIdentity identity) {
        return UserIdentityMapper.toDomain(repository.save(UserIdentityMapper.toEntity(identity)));
    }

    @Override
    public Optional<UserIdentity> findByProviderAndProviderId(AuthProvider provider, String providerId) {
        return repository.findByProviderAndProviderId(provider, providerId).map(UserIdentityMapper::toDomain);
    }

    @Override
    public Optional<UserIdentity> findByUserIdAndProvider(UUID userId, AuthProvider provider) {
        return repository.findByUserIdAndProvider(userId, provider).map(UserIdentityMapper::toDomain);
    }

    @Override
    public List<UserIdentity> findByUserId(UUID userId) {
        return repository.findByUserId(userId).stream().map(UserIdentityMapper::toDomain).toList();
    }

    @Override
    public void delete(UUID identityId) {
        repository.deleteById(identityId);
    }
}
