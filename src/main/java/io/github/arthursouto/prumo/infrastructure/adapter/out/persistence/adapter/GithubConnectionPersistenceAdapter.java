package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.GithubConnectionRepositoryPort;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.GithubConnectionJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.GithubMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class GithubConnectionPersistenceAdapter implements GithubConnectionRepositoryPort {

    private final GithubConnectionJpaRepository repository;

    public GithubConnectionPersistenceAdapter(GithubConnectionJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public GithubConnection save(GithubConnection connection) {
        return GithubMapper.toDomain(repository.save(GithubMapper.toEntity(connection)));
    }

    @Override
    public Optional<GithubConnection> findByUserId(UUID userId) {
        return repository.findById(userId).map(GithubMapper::toDomain);
    }

    @Override
    public void deleteByUserId(UUID userId) {
        repository.deleteById(userId);
    }
}
