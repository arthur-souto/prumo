package io.github.arthursouto.prumo.application.port.out;

import io.github.arthursouto.prumo.domain.model.comment.Comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CommentRepositoryPort {

    Comment save(Comment comment);

    Optional<Comment> findById(UUID id);

    void delete(Comment comment);

    /** The comments written on the story itself, oldest first. */
    List<Comment> findOfStory(UUID storyId);

    /** The comments written on one subtask, oldest first. */
    List<Comment> findOfSubtask(UUID workspaceId, String subtaskKey);
}
