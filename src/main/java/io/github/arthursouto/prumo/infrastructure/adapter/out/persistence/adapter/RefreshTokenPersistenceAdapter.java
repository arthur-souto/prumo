package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.RefreshTokenRepositoryPort;
import io.github.arthursouto.prumo.domain.model.auth.RefreshToken;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.RefreshTokenJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.RefreshTokenMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenPersistenceAdapter implements RefreshTokenRepositoryPort {

    private final RefreshTokenJpaRepository repository;

    public RefreshTokenPersistenceAdapter(RefreshTokenJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        return RefreshTokenMapper.toDomain(repository.save(RefreshTokenMapper.toEntity(refreshToken)));
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return repository.findByTokenHash(tokenHash).map(RefreshTokenMapper::toDomain);
    }

    @Override
    @Transactional
    public void rotate(RefreshToken current, RefreshToken next) {
        repository.save(RefreshTokenMapper.toEntity(current));
        repository.save(RefreshTokenMapper.toEntity(next));
    }

    @Override
    @Transactional
    public void revokeFamily(UUID familyId, Instant revokedAt) {
        repository.revokeFamily(familyId, revokedAt);
    }
}
