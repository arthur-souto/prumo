import type { Column, ColumnKind, Story } from "@/lib/board-types";

export type StatusGroup = "todo" | "doing" | "done";

export const BACKLOG = "backlog";

export function groupOf(kind: ColumnKind | undefined): StatusGroup {
  if (kind === "DONE") return "done";
  if (kind === "IN_PROGRESS" || kind === "IN_REVIEW") return "doing";
  return "todo";
}

export function pointsByGroup(stories: Story[], columns: Map<string, Column>) {
  const totals: Record<StatusGroup, number> = { todo: 0, doing: 0, done: 0 };

  for (const story of stories) {
    totals[groupOf(columns.get(story.columnId)?.kind)] += story.storyPoints ?? 0;
  }

  return totals;
}
