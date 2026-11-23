package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.DeleteWorkspaceCommand;

public interface DeleteWorkspaceUseCase {

    void execute(DeleteWorkspaceCommand command);
}
