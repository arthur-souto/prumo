import { redirect } from "next/navigation";
import { backendFetch } from "./backend";
import { readAccessToken } from "./cookies";
import type { Board, GithubConnection, Repository } from "./board-types";
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
