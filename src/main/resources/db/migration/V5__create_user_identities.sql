CREATE TABLE user_identities (
    id          UUID         PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    provider    VARCHAR(20)  NOT NULL,
    provider_id VARCHAR(100) NOT NULL,
    created_at  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_user_identities_provider UNIQUE (provider, provider_id),
    CONSTRAINT uq_user_identities_user_provider UNIQUE (user_id, provider)
);

INSERT INTO user_identities (id, user_id, provider, provider_id, created_at)
SELECT gen_random_uuid(), id, auth_provider, provider_id, created_at
FROM users
WHERE auth_provider <> 'LOCAL' AND provider_id IS NOT NULL;

ALTER TABLE users DROP CONSTRAINT uq_users_provider;
ALTER TABLE users DROP COLUMN auth_provider;
ALTER TABLE users DROP COLUMN provider_id;
