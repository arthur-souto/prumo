package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceRole;

import java.util.UUID;

public record MemberView(UUID userId, String name, String avatarUrl, WorkspaceRole role) {
}
