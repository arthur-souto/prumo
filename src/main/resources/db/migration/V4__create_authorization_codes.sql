CREATE TABLE authorization_codes (
    id         UUID        PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    code_hash  VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at    TIMESTAMPTZ,
    CONSTRAINT uq_authorization_codes_code_hash UNIQUE (code_hash)
);

CREATE INDEX idx_authorization_codes_user_id ON authorization_codes (user_id);
