import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getT } from "@/i18n/server";
import { requireUser } from "@/lib/session";
import { getBoard, getGithubConnection, getWorkspace, listConnectedRepositories } from "@/lib/workspaces";
import { WorkspaceShell } from "../WorkspaceShell";
import { BoardView } from "./BoardView";

type BoardPageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ refreshed?: string }>;
};

export async function generateMetadata({ params }: BoardPageProps): Promise<Metadata> {
  const workspace = await getWorkspace((await params).id);
  const { t } = await getT();
  return { title: workspace ? `${t("tabs.board")} · ${workspace.name}` : t("meta.workspace") };
}

export default async function BoardPage({ params, searchParams }: BoardPageProps) {
  const { id } = await params;
  const { refreshed } = await searchParams;
  await requireUser(`/workspaces/${id}/board`, refreshed);
  const [workspace, board, repositories, connection ] = await Promise.all([
    getWorkspace(id),
    getBoard(id),
    listConnectedRepositories(id),
    getGithubConnection(),
  ]);

  if (!workspace || !board) {
    notFound();
  }

  return (
    <WorkspaceShell active="board">
      <BoardView
          workspaceId={workspace.id}
          canManageBoard={workspace.role === "OWNER" || workspace.role === "ADMIN"}
          initialBoard={board}
          initialRepositories={repositories}
          hasGithubToken={connection.connected}
      />
    </WorkspaceShell>
  );
}
