CREATE TABLE boards (
    workspace_id       UUID        NOT NULL,
    key_prefix         VARCHAR(10) NOT NULL,
    next_story_number  INTEGER     NOT NULL DEFAULT 1,
    automation_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_boards PRIMARY KEY (workspace_id),
    CONSTRAINT fk_boards_workspace FOREIGN KEY (workspace_id) REFERENCES workspaces (id) ON DELETE CASCADE,
    CONSTRAINT uq_boards_key_prefix UNIQUE (key_prefix)
);

CREATE TABLE board_columns (
    id           UUID        PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES boards (workspace_id) ON DELETE CASCADE,
    name         VARCHAR(60) NOT NULL,
    kind         VARCHAR(20) NOT NULL,
    position     INTEGER     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_board_columns_workspace_id ON board_columns (workspace_id);

CREATE TABLE user_stories (
    id            UUID                  PRIMARY KEY,
    workspace_id  UUID                  NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    story_number  INTEGER               NOT NULL,
    story_key     VARCHAR(20)           NOT NULL,
    title         VARCHAR(200)          NOT NULL,
    description   TEXT,
    priority      VARCHAR(10)           NOT NULL,
    story_points  INTEGER,
    column_id     UUID                  NOT NULL REFERENCES board_columns (id),
    sort_rank     VARCHAR(64) COLLATE "C" NOT NULL,
    repository_id UUID,
    branch_name   VARCHAR(255),
    created_at    TIMESTAMPTZ           NOT NULL,
    updated_at    TIMESTAMPTZ           NOT NULL,
    CONSTRAINT uq_user_stories_key UNIQUE (workspace_id, story_key),
    CONSTRAINT uq_user_stories_number UNIQUE (workspace_id, story_number)
);

CREATE INDEX idx_user_stories_column_rank ON user_stories (column_id, sort_rank);

CREATE TABLE story_labels (
    id       UUID         PRIMARY KEY,
    story_id UUID         NOT NULL REFERENCES user_stories (id) ON DELETE CASCADE,
    label    VARCHAR(40)  NOT NULL,
    position INTEGER      NOT NULL
);

CREATE INDEX idx_story_labels_story_id ON story_labels (story_id);

CREATE TABLE story_checklist_items (
    id       UUID         PRIMARY KEY,
    story_id UUID         NOT NULL REFERENCES user_stories (id) ON DELETE CASCADE,
    kind     VARCHAR(12)  NOT NULL,
    text     VARCHAR(300) NOT NULL,
    done     BOOLEAN      NOT NULL,
    position INTEGER      NOT NULL
);

CREATE INDEX idx_story_checklist_items_story_id ON story_checklist_items (story_id);
