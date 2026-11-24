package io.github.arthursouto.prumo.application.usecase;

import io.github.arthursouto.prumo.application.port.out.CommentRepositoryPort;
import io.github.arthursouto.prumo.domain.exceptions.ForbiddenException;
import io.github.arthursouto.prumo.domain.exceptions.ResourceNotFoundException;
import io.github.arthursouto.prumo.domain.model.comment.Comment;

import java.util.UUID;

/** Finds a comment for a member of its workspace and checks that the user wrote it before it is changed. */
public class CommentAccess {

    private final WorkspaceAccess access;
    private final CommentRepositoryPort commentRepository;

    public CommentAccess(WorkspaceAccess access, CommentRepositoryPort commentRepository) {
        this.access = access;
        this.commentRepository = commentRepository;
    }

    public Comment ownedBy(UUID userId, UUID workspaceId, UUID commentId) {
        access.member(userId, workspaceId);

        Comment comment = commentRepository.findById(commentId)
                .filter(found -> found.getWorkspaceId().equals(workspaceId))
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.isWrittenBy(userId)) {
            throw new ForbiddenException("Only the author can change this comment");
        }

        return comment;
    }
}
