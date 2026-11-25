CREATE TABLE sprint_snapshots (
    id                UUID PRIMARY KEY,
    sprint_id         UUID        NOT NULL REFERENCES sprints (id) ON DELETE CASCADE,
    snapshot_date     DATE        NOT NULL,
    total_points      INTEGER     NOT NULL,
    remaining_points  INTEGER     NOT NULL,
    total_stories     INTEGER     NOT NULL,
    remaining_stories INTEGER     NOT NULL,
    recorded_at       TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_sprint_snapshots_day UNIQUE (sprint_id, snapshot_date)
);
