import type { Board, Column, Story } from "@/lib/board-types";

let sequence = 0;

/** A story with sensible defaults; override only what the test is about. */
export function story(overrides: Partial<Story> = {}): Story {
  sequence += 1;

  return {
    id: `story-${sequence}`,
    key: `PRU-${sequence}`,
    title: `Story ${sequence}`,
    description: null,
    priority: "MEDIUM",
    storyPoints: null,
    columnId: "todo",
    columnSource: "USER",
    labels: [],
    subtasks: null,
    acceptanceCriteria: [],
    repositoryId: null,
    assigneeId: null,
    sprintId: null,
    pullRequests: [],
    createdAt: "2026-11-01T10:00:00Z",
    updatedAt: "2026-11-01T10:00:00Z",
    ...overrides,
  };
}

export const COLUMNS: Column[] = [
  { id: "todo", name: "To Do", kind: "TODO", position: 0 },
  { id: "doing", name: "In Progress", kind: "IN_PROGRESS", position: 1 },
  { id: "review", name: "In Review", kind: "IN_REVIEW", position: 2 },
  { id: "done", name: "Done", kind: "DONE", position: 3 },
];

export function board(overrides: Partial<Board> = {}): Board {
  return { keyPrefix: "PRU", automationEnabled: false, consolidateStories: false, columns: COLUMNS, stories: [], ...overrides };
}
