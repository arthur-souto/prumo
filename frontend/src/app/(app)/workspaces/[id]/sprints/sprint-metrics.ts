import type { Column, Member, Sprint, Story } from "@/lib/board-types";

export const SERIES_LIMIT = 8;

export type StatusSlice = { id: string; name: string; points: number; count: number; slot: number };

export type AssigneeRow = { id: string | null; member: Member | null; done: number; remaining: number; count: number };

export type SprintDays = { total: number; elapsed: number; remaining: number; untilStart: number };

export type SprintMetrics = {
  stories: Story[];
  doneStories: number;
  totalPoints: number;
  donePoints: number;
  byStatus: StatusSlice[];
  byAssignee: AssigneeRow[];
  days: SprintDays;
};

const DAY = 86_400_000;

function epochDay(date: string) {
  return Math.round(Date.parse(`${date}T00:00:00Z`) / DAY);
}

export function localToday() {
  const now = new Date();
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, "0")}-${String(now.getDate()).padStart(2, "0")}`;
}

export function sprintDays(sprint: Sprint, today: string): SprintDays {
  const start = epochDay(sprint.startDate);
  const end = epochDay(sprint.endDate);
  const now = epochDay(today);
  const total = end - start + 1;
  const elapsed = Math.min(total, Math.max(0, now - start + 1));

  return { total, elapsed, remaining: total - elapsed, untilStart: Math.max(0, start - now) };
}

const points = (story: Story) => story.storyPoints ?? 0;

export function sprintMetrics(sprint: Sprint, allStories: Story[], columns: Column[], members: Member[], today: string): SprintMetrics {
  const stories = allStories.filter((story) => story.sprintId === sprint.id);
  const ordered = [...columns].sort((a, b) => a.position - b.position);
  const done = new Set(ordered.filter((column) => column.kind === "DONE").map((column) => column.id));
  const isDone = (story: Story) => done.has(story.columnId);

  const slices = ordered.map((column, index) => {
    const inColumn = stories.filter((story) => story.columnId === column.id);
    return { id: column.id, name: column.name, points: inColumn.reduce((sum, story) => sum + points(story), 0), count: inColumn.length, slot: index + 1 };
  });
  const kept = slices.slice(0, SERIES_LIMIT - (slices.length > SERIES_LIMIT ? 1 : 0));
  const folded = slices.slice(kept.length);
  const byStatus =
    folded.length > 0
      ? [
          ...kept,
          {
            id: "other",
            name: "",
            points: folded.reduce((sum, slice) => sum + slice.points, 0),
            count: folded.reduce((sum, slice) => sum + slice.count, 0),
            slot: SERIES_LIMIT,
          },
        ]
      : kept;

  const rows = new Map<string | null, AssigneeRow>();
  for (const story of stories) {
    const row = rows.get(story.assigneeId) ?? {
      id: story.assigneeId,
      member: members.find((member) => member.id === story.assigneeId) ?? null,
      done: 0,
      remaining: 0,
      count: 0,
    };
    row.count += 1;
    if (isDone(story)) row.done += points(story);
    else row.remaining += points(story);
    rows.set(story.assigneeId, row);
  }
  const byAssignee = [...rows.values()].sort(
    (a, b) => (a.id === null ? 1 : 0) - (b.id === null ? 1 : 0) || b.done + b.remaining - (a.done + a.remaining),
  );

  return {
    stories,
    doneStories: stories.filter(isDone).length,
    totalPoints: stories.reduce((sum, story) => sum + points(story), 0),
    donePoints: stories.filter(isDone).reduce((sum, story) => sum + points(story), 0),
    byStatus,
    byAssignee,
    days: sprintDays(sprint, today),
  };
}

export function percent(part: number, total: number) {
  return total <= 0 ? 0 : Math.min(100, Math.round((part / total) * 100));
}
