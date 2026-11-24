package io.github.arthursouto.prumo.application.port.in;

import io.github.arthursouto.prumo.application.port.in.command.ListCommentsQuery;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;

import java.util.List;

public interface ListCommentsUseCase {

    List<CommentView> execute(ListCommentsQuery command);
}
