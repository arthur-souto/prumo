CREATE TABLE workspaces (
    id          UUID         PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(60)  NOT NULL,
    description VARCHAR(500),
    owner_id    UUID         NOT NULL REFERENCES users (id),
    created_at  TIMESTAMPTZ  NOT NULL,
    updated_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_workspaces_slug UNIQUE (slug)
);

CREATE TABLE workspace_members (
    id           UUID        PRIMARY KEY,
    workspace_id UUID        NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role         VARCHAR(20) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_workspace_members UNIQUE (workspace_id, user_id)
);

CREATE INDEX idx_workspace_members_user_id ON workspace_members (user_id);
