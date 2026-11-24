import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { getT } from "@/i18n/server";
import { requireUser } from "@/lib/session";
import { getGithubConnection, getWorkspace, listConnectedRepositories } from "@/lib/workspaces";
import { GithubSettings } from "../GithubSettings";
import { WorkspaceShell } from "../WorkspaceShell";
import styles from "../workspace.module.css";

type RepositoriesPageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ refreshed?: string }>;
};

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("nav.repositories") };
}

export default async function RepositoriesPage({ params, searchParams }: RepositoriesPageProps) {
  const { id } = await params;
  const { refreshed } = await searchParams;
  await requireUser(`/workspaces/${id}/repositories`, refreshed);
  const [workspace, repositories, connection ] = await Promise.all([
    getWorkspace(id),
    listConnectedRepositories(id),
    getGithubConnection(),
  ]);

  if (!workspace) {
    notFound();
  }

  return (
    <WorkspaceShell active="repositories">
      <div className={styles.grid}>
        <GithubSettings
          workspaceId={workspace.id}
          canEdit={workspace.role === "OWNER" || workspace.role === "ADMIN"}
          connection={connection}
          repositories={repositories}
        />
      </div>
    </WorkspaceShell>
  );
}
