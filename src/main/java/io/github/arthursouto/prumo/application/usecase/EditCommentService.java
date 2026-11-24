package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.in.EditCommentUseCase;
import io.github.arthursouto.prumo.application.port.in.command.EditCommentCommand;
import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;
import io.github.arthursouto.prumo.domain.model.comment.Comment;

public class EditCommentService implements EditCommentUseCase {

    private final CommentAccess comments;
    private final CommentRepositoryPort commentRepository;
    private final CommentAuthors authors;

    public EditCommentService(CommentAccess comments, CommentRepositoryPort commentRepository, CommentAuthors authors) {
        this.comments = comments;
        this.commentRepository = commentRepository;
        this.authors = authors;
    }

    @Override
    public CommentView execute(EditCommentCommand command) {
        Comment comment = comments.ownedBy(command.userId(), command.workspaceId(), command.commentId());

        if (!comment.edit(command.body().strip())) {
            return authors.view(comment);
        }

        return authors.view(commentRepository.save(comment));
    }
}
