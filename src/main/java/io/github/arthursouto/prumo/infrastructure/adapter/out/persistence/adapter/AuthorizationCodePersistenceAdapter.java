package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.AuthorizationCodeRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.AuthorizationCode;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.AuthorizationCodeJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.AuthorizationCodeMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class AuthorizationCodePersistenceAdapter implements AuthorizationCodeRepositoryPort {

    private final AuthorizationCodeJpaRepository repository;

    public AuthorizationCodePersistenceAdapter(AuthorizationCodeJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public AuthorizationCode save(AuthorizationCode authorizationCode) {
        return AuthorizationCodeMapper.toDomain(repository.save(AuthorizationCodeMapper.toEntity(authorizationCode)));
    }

    @Override
    public Optional<AuthorizationCode> findByCodeHash(String codeHash) {
        return repository.findByCodeHash(codeHash).map(AuthorizationCodeMapper::toDomain);
    }

    @Override
    @Transactional
    public boolean markUsed(UUID id, Instant usedAt) {
        return repository.markUsed(id, usedAt) == 1;
    }
}
