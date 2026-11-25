import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getT } from "@/i18n/server";
import { requireUser } from "@/lib/session";
import { getBoard, getGithubConnection, getWorkspace, listConnectedRepositories, listMembers, listSprints } from "@/lib/workspaces";
import { WorkspaceShell } from "../WorkspaceShell";
import { PlanningView } from "./PlanningView";

type SprintsPageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ refreshed?: string }>;
};

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("nav.sprints") };
}

export default async function SprintsPage({ params, searchParams }: SprintsPageProps) {
  const { id } = await params;
  const { refreshed } = await searchParams;
  await requireUser(`/workspaces/${id}/sprints`, refreshed);
  const [workspace, board, members, repositories, connection, sprints] = await Promise.all([
    getWorkspace(id),
    getBoard(id),
    listMembers(id),
    listConnectedRepositories(id),
    getGithubConnection(),
    listSprints(id, ["ACTIVE", "PLANNED"]),
  ]);

  if (!workspace || !board) {
    notFound();
  }

  return (
    <WorkspaceShell active="sprints">
      <PlanningView
        workspaceId={workspace.id}
        board={board}
        members={members}
        repositories={repositories}
        hasGithubToken={connection.connected}
        initialSprints={sprints}
        canManage={workspace.role === "OWNER" || workspace.role === "ADMIN"}
      />
    </WorkspaceShell>
  );
}
