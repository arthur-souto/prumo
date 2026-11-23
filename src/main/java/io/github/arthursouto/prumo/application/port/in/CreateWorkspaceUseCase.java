package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.CreateWorkspaceCommand;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public interface CreateWorkspaceUseCase {

    Workspace execute(CreateWorkspaceCommand command);
}
