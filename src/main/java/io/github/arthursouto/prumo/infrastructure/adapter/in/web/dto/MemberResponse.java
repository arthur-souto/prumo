package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.MemberView;
import io.github.arthursouto.prumo.domain.model.workspace.WorkspaceRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "A member of a workspace")
public record MemberResponse(UUID id, String name, String avatarUrl, WorkspaceRole role) {

    public static MemberResponse from(MemberView view) {
        return new MemberResponse(view.userId(), view.name(), view.avatarUrl(), view.role());
    }
}
