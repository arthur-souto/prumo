package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.GetWorkspaceQuery;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public interface GetWorkspaceUseCase {

    Workspace execute(GetWorkspaceQuery query);
}
