CREATE TABLE github_connections (
    user_id      UUID        PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    token_cipher TEXT        NOT NULL,
    login        VARCHAR(100) NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL
);

CREATE TABLE connected_repositories (
    id              UUID         PRIMARY KEY,
    workspace_id    UUID         NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    owner           VARCHAR(100) NOT NULL,
    name            VARCHAR(100) NOT NULL,
    default_branch  VARCHAR(255) NOT NULL,
    html_url        VARCHAR(500) NOT NULL,
    connected_by    UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL,
    last_synced_at  TIMESTAMPTZ,
    last_sync_error VARCHAR(40),
    etag            VARCHAR(255),
    CONSTRAINT uq_connected_repositories UNIQUE (workspace_id, owner, name)
);

ALTER TABLE user_stories
    ADD CONSTRAINT fk_user_stories_repository FOREIGN KEY (repository_id)
        REFERENCES connected_repositories (id) ON DELETE SET NULL;

CREATE TABLE story_pull_requests (
    id            UUID         PRIMARY KEY,
    story_id      UUID         NOT NULL REFERENCES user_stories (id) ON DELETE CASCADE,
    repository_id UUID         NOT NULL REFERENCES connected_repositories (id) ON DELETE CASCADE,
    number        INTEGER      NOT NULL,
    title         VARCHAR(500) NOT NULL,
    state         VARCHAR(10)  NOT NULL,
    url           VARCHAR(500) NOT NULL,
    branch        VARCHAR(255),
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_story_pull_requests UNIQUE (story_id, repository_id, number)
);

CREATE INDEX idx_story_pull_requests_story_id ON story_pull_requests (story_id);
