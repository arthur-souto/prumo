package io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.mapper;

import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.github.arthursouto.prumo.domain.model.github.GithubConnection;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.ConnectedRepositoryEntity;
import io.github.arthursouto.prumo.infrastructure.adapter.out.persistence.entity.GithubConnectionEntity;

public final class GithubMapper {

    private GithubMapper() {
    }

    public static GithubConnectionEntity toEntity(GithubConnection connection) {
        GithubConnectionEntity entity = new GithubConnectionEntity();
        entity.setUserId(connection.userId());
        entity.setTokenCipher(connection.tokenCipher());
        entity.setLogin(connection.login());
        entity.setCreatedAt(connection.createdAt());
        return entity;
    }

    public static GithubConnection toDomain(GithubConnectionEntity entity) {
        return new GithubConnection(
                entity.getUserId(),
                entity.getTokenCipher(),
                entity.getLogin(),
                entity.getCreatedAt()
        );
    }

    public static ConnectedRepositoryEntity toEntity(ConnectedRepository repository) {
        ConnectedRepositoryEntity entity = new ConnectedRepositoryEntity();
        entity.setId(repository.getId());
        entity.setWorkspaceId(repository.getWorkspaceId());
        entity.setOwner(repository.getOwner());
        entity.setName(repository.getName());
        entity.setDefaultBranch(repository.getDefaultBranch());
        entity.setHtmlUrl(repository.getHtmlUrl());
        entity.setConnectedBy(repository.getConnectedBy());
        entity.setCreatedAt(repository.getCreatedAt());
        entity.setLastSyncedAt(repository.getLastSyncedAt());
        entity.setLastSyncError(repository.getLastSyncError());
        entity.setEtag(repository.getEtag());
        return entity;
    }

    public static ConnectedRepository toDomain(ConnectedRepositoryEntity entity) {
        return ConnectedRepository.restore(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getOwner(),
                entity.getName(),
                entity.getDefaultBranch(),
                entity.getHtmlUrl(),
                entity.getConnectedBy(),
                entity.getCreatedAt(),
                entity.getLastSyncedAt(),
                entity.getLastSyncError(),
                entity.getEtag()
        );
    }
}
