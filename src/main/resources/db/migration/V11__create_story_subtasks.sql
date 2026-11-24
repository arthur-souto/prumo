-- Subtasks become full entities that own the branch. Existing checklist subtasks and
-- story-level branches are carried over, so no branch or pull request is left orphaned.

CREATE TABLE story_subtasks (
    id             UUID         PRIMARY KEY,
    story_id       UUID         NOT NULL REFERENCES user_stories (id) ON DELETE CASCADE,
    workspace_id   UUID         NOT NULL REFERENCES workspaces (id) ON DELETE CASCADE,
    subtask_number INTEGER      NOT NULL,
    subtask_key    VARCHAR(20)  NOT NULL,
    title          VARCHAR(300) NOT NULL,
    description    TEXT,
    status         VARCHAR(12)  NOT NULL,
    position       INTEGER      NOT NULL,
    branch_name    VARCHAR(255),
    created_at     TIMESTAMPTZ  NOT NULL,
    updated_at     TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_story_subtasks_key UNIQUE (workspace_id, subtask_key),
    CONSTRAINT uq_story_subtasks_number UNIQUE (workspace_id, subtask_number)
);

CREATE INDEX idx_story_subtasks_story_id ON story_subtasks (story_id, position);

ALTER TABLE story_pull_requests
    ADD COLUMN subtask_id UUID REFERENCES story_subtasks (id) ON DELETE SET NULL;

CREATE INDEX idx_story_pull_requests_subtask_id ON story_pull_requests (subtask_id);

-- 1. Rows to create: one per checklist subtask, plus "Initial implementation" for each story that had a branch.
CREATE TEMPORARY TABLE tmp_new_subtasks ON COMMIT DROP AS
SELECT gen_random_uuid()                          AS id,
       s.id                                       AS story_id,
       s.workspace_id                             AS workspace_id,
       i.text                                     AS title,
       CASE WHEN i.done THEN 'DONE' ELSE 'TO_DO' END AS status,
       i.position                                 AS position,
       CAST(NULL AS VARCHAR(255))                 AS branch_name,
       s.created_at                               AS created_at,
       0                                          AS origin
FROM story_checklist_items i
         JOIN user_stories s ON s.id = i.story_id
WHERE i.kind = 'SUBTASK'
UNION ALL
SELECT gen_random_uuid(),
       s.id,
       s.workspace_id,
       'Implementação inicial',
       CASE
           WHEN EXISTS (SELECT 1 FROM story_pull_requests pr
                        WHERE pr.story_id = s.id AND pr.branch = s.branch_name AND pr.state = 'MERGED') THEN 'DONE'
           WHEN EXISTS (SELECT 1 FROM story_pull_requests pr
                        WHERE pr.story_id = s.id AND pr.branch = s.branch_name AND pr.state = 'OPEN') THEN 'IN_REVIEW'
           ELSE 'IN_PROGRESS'
       END,
       COALESCE((SELECT MAX(c.position) + 1 FROM story_checklist_items c
                 WHERE c.story_id = s.id AND c.kind = 'SUBTASK'), 0),
       s.branch_name,
       s.created_at,
       1
FROM user_stories s
WHERE s.branch_name IS NOT NULL;

-- 2. Keys continue the workspace sequence, so they never collide with a story or another subtask.
CREATE TEMPORARY TABLE tmp_numbered_subtasks ON COMMIT DROP AS
SELECT t.*,
       b.key_prefix,
       b.next_story_number - 1 + ROW_NUMBER() OVER (
           PARTITION BY t.workspace_id
           ORDER BY t.created_at, t.story_id, t.origin, t.position
       ) AS number
FROM tmp_new_subtasks t
         JOIN boards b ON b.workspace_id = t.workspace_id;

INSERT INTO story_subtasks (id, story_id, workspace_id, subtask_number, subtask_key, title, description,
                            status, position, branch_name, created_at, updated_at)
SELECT id, story_id, workspace_id, number, key_prefix || '-' || number, title, NULL,
       status, position, branch_name, created_at, created_at
FROM tmp_numbered_subtasks;

UPDATE boards b
SET next_story_number = b.next_story_number + c.total
FROM (SELECT workspace_id, COUNT(*) AS total FROM tmp_numbered_subtasks GROUP BY workspace_id) c
WHERE c.workspace_id = b.workspace_id;

-- 3. Pull requests opened from the story's branch follow it to the new subtask.
UPDATE story_pull_requests pr
SET subtask_id = n.id
FROM tmp_numbered_subtasks n
         JOIN user_stories s ON s.id = n.story_id
WHERE n.origin = 1
  AND pr.story_id = s.id
  AND pr.branch = s.branch_name;

-- 4. The old representations go away.
DELETE FROM story_checklist_items WHERE kind = 'SUBTASK';

ALTER TABLE user_stories DROP COLUMN branch_name;
