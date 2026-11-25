import { redirect } from "next/navigation";
import { backendFetch } from "./backend";
import { readAccessToken } from "./cookies";
import type { Board, GithubConnection, Repository, Member, Sprint, SprintPage, SprintState, Story, Subtask } from "./board-types";
import type { Workspace } from "./workspace-types";

async function authedGet(path: string) {
  const token = await readAccessToken();
  const response = await backendFetch(path, {
    headers: { Authorization: `Bearer ${token ?? ""}` },
  }).catch(() => null);

  if (response?.status === 401) {
    redirect("/api/auth/refresh?clear=1");
  }

  return response;
}

export async function listWorkspaces(): Promise<Workspace[]> {
  const response = await authedGet("/api/v1/workspaces");

  return response?.ok ? ((await response.json()) as Workspace[]) : [];
}

export async function getWorkspace(id: string): Promise<Workspace | null> {
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(id)}`);

  return response?.ok ? ((await response.json()) as Workspace) : null;
}

export async function getBoard(workspaceId: string): Promise<Board | null> {
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/board`);

  return response?.ok ? ((await response.json()) as Board) : null;
}

export async function listConnectedRepositories(workspaceId: string): Promise<Repository[]> {
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/repositories`);

  return response?.ok ? ((await response.json()) as Repository[]) : [];
}

export async function getGithubConnection(): Promise<GithubConnection> {
  const response = await authedGet("/api/v1/github/connection");

  return response?.ok ? ((await response.json()) as GithubConnection) : { connected: false, login: null, connectedAt: null };
}

export async function listMembers(workspaceId: string): Promise<Member[]> {
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/members`);

  return response?.ok ? ((await response.json()) as Member[]) : [];
}

export async function listSprints(workspaceId: string, states: SprintState[] = []): Promise<Sprint[]> {
  const query = new URLSearchParams({ size: "100" });
  states.forEach((state) => query.append("state", state));
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/sprints?${query}`);

  return response?.ok ? ((await response.json()) as SprintPage).items : [];
}

export async function getSubtask(workspaceId: string, key: string): Promise<Subtask | null> {
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/subtasks/${encodeURIComponent(key)}`);

  return response?.ok ? ((await response.json()) as Subtask) : null;
}

export async function getStory(workspaceId: string, key: string): Promise<Story | null> {
  const response = await authedGet(`/api/v1/workspaces/${encodeURIComponent(workspaceId)}/stories/${encodeURIComponent(key)}`);

  return response?.ok ? ((await response.json()) as Story) : null;
}
