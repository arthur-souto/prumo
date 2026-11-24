package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.AddCommentCommand;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;

public interface AddCommentUseCase {

    CommentView execute(AddCommentCommand command);
}
