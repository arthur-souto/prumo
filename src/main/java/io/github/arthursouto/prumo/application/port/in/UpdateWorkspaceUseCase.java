package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.UpdateWorkspaceCommand;
import io.github.arthursouto.prumo.domain.model.workspace.Workspace;

public interface UpdateWorkspaceUseCase {

    Workspace execute(UpdateWorkspaceCommand command);
}
