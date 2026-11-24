ALTER TABLE story_subtasks
    ADD COLUMN status_source VARCHAR(10) NOT NULL DEFAULT 'USER';

ALTER TABLE user_stories
    ADD COLUMN column_source VARCHAR(10) NOT NULL DEFAULT 'USER';
