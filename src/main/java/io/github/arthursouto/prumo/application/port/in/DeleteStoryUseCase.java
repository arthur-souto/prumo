package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.DeleteStoryCommand;

public interface DeleteStoryUseCase {

    void execute(DeleteStoryCommand command);
}
