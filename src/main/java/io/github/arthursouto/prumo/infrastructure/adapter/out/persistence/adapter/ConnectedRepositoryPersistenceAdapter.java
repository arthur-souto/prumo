package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.adapter;

import io.github.arthursouto.prumo.application.port.out.ConnectedRepositoryPort;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.jpa.ConnectedRepositoryJpaRepository;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper.GithubMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ConnectedRepositoryPersistenceAdapter implements ConnectedRepositoryPort {

    private final ConnectedRepositoryJpaRepository repository;

    public ConnectedRepositoryPersistenceAdapter(ConnectedRepositoryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public ConnectedRepository save(ConnectedRepository connectedRepository) {
        return GithubMapper.toDomain(repository.save(GithubMapper.toEntity(connectedRepository)));
    }

    @Override
    public Optional<ConnectedRepository> findById(UUID id) {
        return repository.findById(id).map(GithubMapper::toDomain);
    }

    @Override
    public List<ConnectedRepository> findAllByWorkspaceId(UUID workspaceId) {
        return repository.findByWorkspaceIdOrderByCreatedAt(workspaceId).stream().map(GithubMapper::toDomain).toList();
    }

    @Override
    public List<ConnectedRepository> findAll() {
        return repository.findAll().stream().map(GithubMapper::toDomain).toList();
    }

    @Override
    public Optional<ConnectedRepository> findByWorkspaceIdAndOwnerAndName(UUID workspaceId, String owner, String name) {
        return repository.findByWorkspaceIdAndOwnerIgnoreCaseAndNameIgnoreCase(workspaceId, owner, name)
                .map(GithubMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        repository.deleteById(id);
    }
}
