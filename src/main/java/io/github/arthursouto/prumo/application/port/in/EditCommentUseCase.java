package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.EditCommentCommand;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;

public interface EditCommentUseCase {

    CommentView execute(EditCommentCommand command);
}
