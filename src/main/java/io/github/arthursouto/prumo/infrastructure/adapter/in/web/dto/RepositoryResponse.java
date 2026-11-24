package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.out.result.RemoteRepository;
import io.github.arthursouto.prumo.domain.model.github.ConnectedRepository;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A GitHub repository, connected to a workspace or only accessible to the user")
public record RepositoryResponse(
        UUID id,
        String owner,
        String name,
        String fullName,
        String defaultBranch,
        String htmlUrl,
        boolean isPrivate,
        Instant lastSyncedAt,
        String lastSyncError
) {

    public static RepositoryResponse from(ConnectedRepository repository) {
        return new RepositoryResponse(
                repository.getId(),
                repository.getOwner(),
                repository.getName(),
                repository.fullName(),
                repository.getDefaultBranch(),
                repository.getHtmlUrl(),
                false,
                repository.getLastSyncedAt(),
                repository.getLastSyncError()
        );
    }

    public static RepositoryResponse from(RemoteRepository repository) {
        return new RepositoryResponse(
                null,
                repository.owner(),
                repository.name(),
                repository.owner() + "/" + repository.name(),
                repository.defaultBranch(),
                repository.htmlUrl(),
                repository.isPrivate(),
                null,
                null
        );
    }
}
