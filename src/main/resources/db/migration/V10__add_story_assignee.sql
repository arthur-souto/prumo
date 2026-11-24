ALTER TABLE user_stories
    ADD COLUMN assignee_id UUID REFERENCES users (id) ON DELETE SET NULL;

CREATE INDEX idx_user_stories_assignee_id ON user_stories (assignee_id);
