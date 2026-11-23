CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    name          VARCHAR(150) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    role          VARCHAR(20)  NOT NULL,
    auth_provider VARCHAR(20)  NOT NULL,
    provider_id   VARCHAR(100),
    avatar_url    VARCHAR(500),
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT uq_users_provider UNIQUE (auth_provider, provider_id)
);
