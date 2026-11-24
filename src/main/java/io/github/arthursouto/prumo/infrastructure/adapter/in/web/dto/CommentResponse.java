package io.github.arthursouto.prumo.infrastructure.adapter.in.web.dto;

import io.github.arthursouto.prumo.application.port.in.result.CommentView;
import io.github.arthursouto.prumo.domain.model.comment.Comment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

@Schema(description = "A comment on a story or subtask")
public record CommentResponse(
        UUID id,
        UUID authorId,
        @Schema(description = "Null when the author's account no longer exists") String authorName,
        String authorAvatarUrl,
        @Schema(description = "Set when the comment is on a subtask; null on a story") String subtaskKey,
        @Schema(description = "Markdown") String body,
        Instant createdAt,
        @Schema(description = "Null until the text is edited") Instant editedAt,
        @Schema(description = "True when the caller wrote it, and so can edit or delete it") boolean mine
) {

    public static CommentResponse from(CommentView view, UUID callerId) {
        Comment comment = view.comment();

        return new CommentResponse(
                comment.getId(),
                comment.getAuthorId(),
                view.authorName(),
                view.authorAvatarUrl(),
                comment.getSubtaskKey(),
                comment.getBody(),
                comment.getCreatedAt(),
                comment.getEditedAt(),
                comment.isWrittenBy(callerId)
        );
    }
}
