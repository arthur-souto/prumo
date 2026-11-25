export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type ColumnKind = "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE" | "CUSTOM";
export type PullRequestState = "OPEN" | "MERGED" | "CLOSED";

export const PRIORITIES: Priority[] = ["URGENT", "HIGH", "MEDIUM", "LOW"];

export type ChecklistItem = { id: string | null; text: string; done: boolean };

export type ChangeSource = "USER" | "GITHUB";

export type SubtaskStatus = "TO_DO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE";

export const SUBTASK_STATUSES: SubtaskStatus[] = ["TO_DO", "IN_PROGRESS", "IN_REVIEW", "DONE"];

export type PullRequest = {
  subtaskId?: string | null;
  number: number;
  title: string;
  state: PullRequestState;
  url: string;
  branch: string | null;
  repositoryId: string;
};

export type SubtaskProgress = { total: number; done: number };

export type Subtask = {
  id: string;
  key: string;
  storyKey: string;
  title: string;
  description: string | null;
  status: SubtaskStatus;
  /** Who last changed the status: a person on the board, or the GitHub pull request sync. */
  statusSource: ChangeSource;
  position: number;
  branchName: string | null;
  pullRequests: PullRequest[];
  createdAt: string;
  updatedAt: string;
};

export type Story = {
  id: string;
  key: string;
  title: string;
  description: string | null;
  priority: Priority;
  storyPoints: number | null;
  columnId: string;
  /** Who last moved the story: a person on the board, or the GitHub pull request sync. */
  columnSource: ChangeSource;
  labels: string[];
  /** Progress of the story's subtasks; absent when the endpoint does not report it (e.g. after a single-story update). */
  subtasks?: SubtaskProgress | null;
  acceptanceCriteria: ChecklistItem[];
  repositoryId: string | null;
  assigneeId: string | null;
  /** Null while the story is in the backlog. */
  sprintId: string | null;
  pullRequests: PullRequest[];
  createdAt: string;
  updatedAt: string;
};

export type SprintState = "PLANNED" | "ACTIVE" | "CLOSED";

export type SprintPage = { items: Sprint[]; page: number; size: number; totalItems: number; totalPages: number };

export type Sprint = {
  id: string;
  name: string;
  goal: string | null;
  startDate: string;
  endDate: string;
  state: SprintState;
  /** Points in the sprint when it started; null until then. */
  committedPoints: number | null;
  /** Points done when it closed; null until then. */
  completedPoints: number | null;
  storyCount: number;
  totalPoints: number;
  donePoints: number;
  startedAt: string | null;
  closedAt: string | null;
  createdAt: string;
  updatedAt: string;
};

export type Column = { id: string; name: string; kind: ColumnKind; position: number };

export type Board = {
  keyPrefix: string;
  automationEnabled: boolean;
  consolidateStories: boolean;
  columns: Column[];
  stories: Story[];
};

export type Repository = {
  id: string | null;
  owner: string;
  name: string;
  fullName: string;
  defaultBranch: string;
  htmlUrl: string;
  isPrivate: boolean;
  lastSyncedAt: string | null;
  lastSyncError: string | null;
};

export type GithubConnection = { connected: boolean; login: string | null; connectedAt: string | null };

export type Member = {
  id: string;
  name: string;
  avatarUrl: string | null;
  role: "OWNER" | "ADMIN" | "MEMBER";
};

export type ActivityType =
  | "STORY_CREATED"
  | "STORY_MOVED"
  | "STORY_UPDATED"
  | "STORY_POINTS_CHANGED"
  | "STORY_ASSIGNED"
  | "SUBTASK_ADDED"
  | "SUBTASK_STATUS_CHANGED";

/** One entry of the history of a story or subtask. The actor is null when GitHub made the change. */
export type ActivityEntry = {
  id: string;
  type: ActivityType;
  source: ChangeSource;
  actorId: string | null;
  actorName: string | null;
  subtaskKey: string | null;
  from: string | null;
  to: string | null;
  createdAt: string;
};

export type Comment = {
  id: string;
  authorId: string | null;
  authorName: string | null;
  authorAvatarUrl: string | null;
  subtaskKey: string | null;
  body: string;
  createdAt: string;
  editedAt: string | null;
  /** True when the caller wrote it, and so can edit or delete it. */
  mine: boolean;
};

export type BurndownDay = {
  date: string;
  totalPoints: number | null;
  remainingPoints: number | null;
  totalStories: number | null;
  remainingStories: number | null;
  idealPoints: number;
};

export type SprintBurndown = {
  sprintId: string;
  state: SprintState;
  startDate: string;
  endDate: string;
  committedPoints: number | null;
  completedPoints: number | null;
  baselinePoints: number;
  burndown: BurndownDay[];
};
