export type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";
export type ColumnKind = "TODO" | "IN_PROGRESS" | "IN_REVIEW" | "DONE" | "CUSTOM";
export type PullRequestState = "OPEN" | "MERGED" | "CLOSED";

export const PRIORITIES: Priority[] = ["URGENT", "HIGH", "MEDIUM", "LOW"];

export type ChecklistItem = { id: string | null; text: string; done: boolean };

export type PullRequest = {
  number: number;
  title: string;
  state: PullRequestState;
  url: string;
  branch: string | null;
  repositoryId: string;
};

export type Story = {
  id: string;
  key: string;
  title: string;
  description: string | null;
  priority: Priority;
  storyPoints: number | null;
  columnId: string;
  labels: string[];
  subtasks: ChecklistItem[];
  acceptanceCriteria: ChecklistItem[];
  repositoryId: string | null;
  branchName: string | null;
  pullRequests: PullRequest[];
  createdAt: string;
  updatedAt: string;
};

export type Column = { id: string; name: string; kind: ColumnKind; position: number };

export type Board = {
  keyPrefix: string;
  automationEnabled: boolean;
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
