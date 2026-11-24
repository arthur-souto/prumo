package io.github.arthursouto.prumo.application.port.in.result;

import io.github.arthursouto.prumo.domain.model.comment.Comment;

/** A comment with its author's profile resolved; the name is null when the account no longer exists. */
public record CommentView(Comment comment, String authorName, String authorAvatarUrl) {
}
