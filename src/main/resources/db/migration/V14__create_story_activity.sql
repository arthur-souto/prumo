CREATE TABLE story_activity (
    id           UUID PRIMARY KEY,
    workspace_id UUID         NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    story_id     UUID         NOT NULL REFERENCES user_stories (id) ON DELETE CASCADE,
    subtask_key  VARCHAR(20),
    actor_id     UUID         REFERENCES users (id) ON DELETE SET NULL,
    source       VARCHAR(10)  NOT NULL,
    type         VARCHAR(30)  NOT NULL,
    from_value   VARCHAR(300),
    to_value     VARCHAR(300),
    created_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_story_activity_story ON story_activity (story_id, created_at DESC);
CREATE INDEX idx_story_activity_subtask ON story_activity (workspace_id, subtask_key, created_at DESC);
