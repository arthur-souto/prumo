CREATE TABLE story_comments (
    id           UUID PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    story_id     UUID        NOT NULL REFERENCES user_stories (id) ON DELETE CASCADE,
    subtask_key  VARCHAR(20),
    author_id    UUID        REFERENCES users (id) ON DELETE SET NULL,
    body         TEXT        NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    edited_at    TIMESTAMPTZ
);

CREATE INDEX idx_story_comments_story ON story_comments (story_id, created_at);
CREATE INDEX idx_story_comments_subtask ON story_comments (workspace_id, subtask_key, created_at);
