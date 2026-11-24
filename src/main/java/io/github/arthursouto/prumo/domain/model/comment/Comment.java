package io.github.arthursouto.prumo.domain.model.comment;

import java.time.Instant;
import java.util.UUID;

/** A Markdown comment on a story or, with a subtask key, on one of its subtasks. Only its author edits or deletes it. */
public final class Comment {

    private final UUID id;
    private final UUID workspaceId;
    private final UUID storyId;
    private final String subtaskKey;
    private final UUID authorId;
    private String body;
    private final Instant createdAt;
    private Instant editedAt;

    private Comment(UUID id,
                    UUID workspaceId,
                    UUID storyId,
                    String subtaskKey,
                    UUID authorId,
                    String body,
                    Instant createdAt,
                    Instant editedAt) {
        this.id = id;
        this.workspaceId = workspaceId;
        this.storyId = storyId;
        this.subtaskKey = subtaskKey;
        this.authorId = authorId;
        this.body = body;
        this.createdAt = createdAt;
        this.editedAt = editedAt;
    }

    public static Comment create(UUID workspaceId, UUID storyId, String subtaskKey, UUID authorId, String body) {
        return new Comment(UUID.randomUUID(), workspaceId, storyId, subtaskKey, authorId, body, Instant.now(), null);
    }

    public static Comment restore(UUID id,
                                  UUID workspaceId,
                                  UUID storyId,
                                  String subtaskKey,
                                  UUID authorId,
                                  String body,
                                  Instant createdAt,
                                  Instant editedAt) {
        return new Comment(id, workspaceId, storyId, subtaskKey, authorId, body, createdAt, editedAt);
    }

    /** Returns true when the text actually changed; saving the same text leaves no "edited" mark. */
    public boolean edit(String body) {
        if (this.body.equals(body)) {
            return false;
        }

        this.body = body;
        this.editedAt = Instant.now();
        return true;
    }

    public boolean isWrittenBy(UUID userId) {
        return authorId != null && authorId.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public UUID getStoryId() {
        return storyId;
    }

    public String getSubtaskKey() {
        return subtaskKey;
    }

    public UUID getAuthorId() {
        return authorId;
    }

    public String getBody() {
        return body;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getEditedAt() {
        return editedAt;
    }
}
