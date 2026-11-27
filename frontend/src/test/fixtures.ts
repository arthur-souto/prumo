import type { Board, Column, Member, Repository, Sprint, Story, Subtask } from "@/lib/board-types";

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

export function member(overrides: Partial<Member> = {}): Member {
  sequence += 1;

  return { id: `member-${sequence}`, name: `Member ${sequence}`, avatarUrl: null, role: "MEMBER", ...overrides };
}

export function sprint(overrides: Partial<Sprint> = {}): Sprint {
  sequence += 1;

  return {
    id: `sprint-${sequence}`,
    name: `Sprint ${sequence}`,
    goal: null,
    startDate: "2026-12-01",
    endDate: "2026-12-14",
    state: "PLANNED",
    committedPoints: null,
    completedPoints: null,
    storyCount: 0,
    totalPoints: 0,
    donePoints: 0,
    startedAt: null,
    closedAt: null,
    createdAt: "2026-11-01T10:00:00Z",
    updatedAt: "2026-11-01T10:00:00Z",
    ...overrides,
  };
}

export function repository(overrides: Partial<Repository> = {}): Repository {
  return {
    id: "repo-1",
    owner: "arthur",
    name: "prumo",
    fullName: "arthur/prumo",
    defaultBranch: "main",
    htmlUrl: "https://github.com/arthur/prumo",
    isPrivate: true,
    lastSyncedAt: null,
    lastSyncError: null,
    ...overrides,
  };
}

export function subtask(overrides: Partial<Subtask> = {}): Subtask {
  sequence += 1;

  return {
    id: `subtask-${sequence}`,
    key: `PRU-${sequence}`,
    storyKey: "PRU-1",
    title: `Subtask ${sequence}`,
    description: null,
    status: "TO_DO",
    statusSource: "USER",
    position: sequence,
    branchName: null,
    pullRequests: [],
    createdAt: "2026-11-01T10:00:00Z",
    updatedAt: "2026-11-01T10:00:00Z",
    ...overrides,
  };
}

/** A page of the story search, as GET /stories answers it. */
export function storyPage(items: Story[], { page = 0, size = 25, totalItems = items.length } = {}) {
  return { items, page, size, totalItems, totalPages: Math.ceil(totalItems / size) };
}
