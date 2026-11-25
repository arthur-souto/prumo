CREATE TABLE sprints (
    id               UUID PRIMARY KEY,
    workspace_id     UUID         NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    name             VARCHAR(80)  NOT NULL,
    goal             VARCHAR(500),
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    state            VARCHAR(10)  NOT NULL,
    committed_points INTEGER,
    completed_points INTEGER,
    started_at       TIMESTAMPTZ,
    closed_at        TIMESTAMPTZ,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_sprints_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_sprints_workspace ON sprints (workspace_id, start_date);
CREATE UNIQUE INDEX uq_sprints_one_active ON sprints (workspace_id) WHERE state = 'ACTIVE';

ALTER TABLE user_stories
    ADD COLUMN sprint_id UUID REFERENCES sprints (id) ON DELETE SET NULL;

CREATE INDEX idx_user_stories_sprint ON user_stories (sprint_id);
