package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.DeleteCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.command.DeleteCommentCommand;
import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;

public class DeleteCommentService implements DeleteCommentUseCase {

    private final CommentAccess comments;
    private final CommentRepositoryPort commentRepository;

    public DeleteCommentService(CommentAccess comments, CommentRepositoryPort commentRepository) {
        this.comments = comments;
        this.commentRepository = commentRepository;
    }

    @Override
    public void execute(DeleteCommentCommand command) {
        commentRepository.delete(comments.ownedBy(command.userId(), command.workspaceId(), command.commentId()));
    }
}
