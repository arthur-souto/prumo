import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getT } from "@/i18n/server";
import { requireUser } from "@/lib/session";
import { getStory, getSubtask, getWorkspace, listConnectedRepositories } from "@/lib/workspaces";
import { SubtaskView } from "./SubtaskView";

type SubtaskPageProps = {
  params: Promise<{ id: string; key: string }>;
  searchParams: Promise<{ refreshed?: string }>;
};

export async function generateMetadata({ params }: SubtaskPageProps): Promise<Metadata> {
  const { id, key } = await params;
  const subtask = await getSubtask(id, key);
  const { t } = await getT();

  return { title: subtask ? `${subtask.key} · ${subtask.title}` : t("meta.workspace") };
}

export default async function SubtaskPage({ params, searchParams }: SubtaskPageProps) {
  const { id, key } = await params;
  const { refreshed } = await searchParams;
  await requireUser(`/workspaces/${id}/subtasks/${key}`, refreshed);
  const [workspace, subtask, repositories] = await Promise.all([getWorkspace(id), getSubtask(id, key), listConnectedRepositories(id)]);
  const story = subtask ? await getStory(id, subtask.storyKey) : null;

  if (!workspace || !subtask || !story) {
    notFound();
  }

  return (
    <SubtaskView
      workspaceId={workspace.id}
      initial={subtask}
      storyTitle={story.title}
      repository={repositories.find((candidate) => candidate.id === story.repositoryId)}
      canBranch={story.repositoryId !== null}
    />
  );
}
