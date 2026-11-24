import type { Metadata } from "next";
import { getBoard, listWorkspaces } from "@/lib/workspaces";
import { getT } from "@/i18n/server";
import { dateLocale } from "@/i18n/core";
import { formatDate } from "@/lib/workspace-types";
import { requireUser } from "@/lib/session";
import { WorkspacesView } from "./WorkspacesView";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("meta.workspaces") };
}

type HomeProps = {
  searchParams: Promise<{ refreshed?: string }>;
};

export default async function HomePage({ searchParams }: HomeProps) {
  const { refreshed } = await searchParams;
  await requireUser("/", refreshed);
  const workspaces = await listWorkspaces();
  const { locale } = await getT();
  // Progress = stories in the last column (the "done" lane) over all stories.
  const boards = await Promise.all(workspaces.map((workspace) => getBoard(workspace.id)));
  const progress = workspaces.map((_, index) => {
    const board = boards[index];
    if (!board || board.stories.length === 0) return { total: 0, done: 0 };
    const last = [...board.columns].sort((a, b) => a.position - b.position).at(-1);
    return { total: board.stories.length, done: board.stories.filter((story) => story.columnId === last?.id).length };
  });

  return (
    <WorkspacesView workspaces={workspaces.map((workspace, index) => ({ ...workspace, ...progress[index], createdLabel: formatDate(workspace.createdAt, dateLocale(locale)) }))} />
  );
}
