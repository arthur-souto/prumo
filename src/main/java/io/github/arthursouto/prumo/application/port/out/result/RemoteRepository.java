package io.github.arthursouto.prumo.application.port.out.result;

public record RemoteRepository(
        String owner,
        String name,
        String defaultBranch,
        String htmlUrl,
        boolean isPrivate,
        String description
) {
}
