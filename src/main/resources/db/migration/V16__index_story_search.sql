CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE FUNCTION immutable_unaccent(value text) RETURNS text
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
    RETURN public.unaccent('public.unaccent'::regdictionary, value);

CREATE INDEX idx_user_stories_title_trgm ON user_stories USING gin (immutable_unaccent(lower(title)) gin_trgm_ops);
CREATE INDEX idx_user_stories_key_trgm ON user_stories USING gin (lower(story_key) gin_trgm_ops);
CREATE INDEX idx_user_stories_description_trgm ON user_stories USING gin (immutable_unaccent(lower(description)) gin_trgm_ops);

CREATE INDEX idx_user_stories_workspace_updated ON user_stories (workspace_id, updated_at);

CREATE INDEX idx_story_labels_lower_label ON story_labels (lower(label), story_id);
