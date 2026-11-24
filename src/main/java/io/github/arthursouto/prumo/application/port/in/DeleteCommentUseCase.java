package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.DeleteCommentCommand;

public interface DeleteCommentUseCase {

    void execute(DeleteCommentCommand command);
}
