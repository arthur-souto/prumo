-- Manual rollback for V11 (Flyway Community has no automatic undo). Run it by hand, then delete the
-- V11 row from flyway_schema_history. Keys already handed out are not returned to the sequence.

ALTER TABLE user_stories ADD COLUMN branch_name VARCHAR(255);

-- Subtasks go back to the story checklist; "done" is a subtask in DONE.
INSERT INTO story_checklist_items (id, story_id, kind, text, done, position)
SELECT id, story_id, 'SUBTASK', LEFT(title, 300), status = 'DONE',
       ROW_NUMBER() OVER (PARTITION BY story_id ORDER BY position) - 1
FROM story_subtasks;

-- A story gets back one branch: the first subtask that had one.
UPDATE user_stories s
SET branch_name = x.branch_name
FROM (SELECT DISTINCT ON (story_id) story_id, branch_name
      FROM story_subtasks
      WHERE branch_name IS NOT NULL
      ORDER BY story_id, position) x
WHERE x.story_id = s.id;

DROP INDEX idx_story_pull_requests_subtask_id;
ALTER TABLE story_pull_requests DROP COLUMN subtask_id;
DROP TABLE story_subtasks;
