import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getT } from "@/i18n/server";
import { requireUser } from "@/lib/session";
import { getBoard, getGithubConnection, getWorkspace, listConnectedRepositories, listMembers, listSprints } from "@/lib/workspaces";
import { WorkspaceShell } from "../WorkspaceShell";
import { BacklogView } from "./BacklogView";

type BacklogPageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ refreshed?: string }>;
};

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("nav.backlog") };
}

export default async function BacklogPage({ params, searchParams }: BacklogPageProps) {
  const { id } = await params;
  const { refreshed } = await searchParams;
  await requireUser(`/workspaces/${id}/backlog`, refreshed);
  const [workspace, board, repositories, connection, members, sprints] = await Promise.all([
    getWorkspace(id),
    getBoard(id),
    listConnectedRepositories(id),
    getGithubConnection(),
    listMembers(id),
    listSprints(id),
  ]);

  if (!workspace || !board) {
    notFound();
  }

  return (
    <WorkspaceShell active="backlog">
      <BacklogView members={members} sprints={sprints} workspaceId={workspace.id} board={board} repositories={repositories} hasGithubToken={connection.connected} />
    </WorkspaceShell>
  );
}
