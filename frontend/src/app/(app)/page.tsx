import type { Metadata } from "next";
import { listWorkspaces } from "@/lib/workspaces";
import { getT } from "@/i18n/server";
import { dateLocale } from "@/i18n/core";
import { formatDate } from "@/lib/workspace-types";
import { requireUser } from "@/lib/session";
import { TopNav } from "./TopNav";
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
  const user = await requireUser("/", refreshed);
  const workspaces = await listWorkspaces();
  const { locale } = await getT();

  return (
    <>
      <TopNav name={user.name} email={user.email} />
      <WorkspacesView workspaces={workspaces.map((workspace) => ({ ...workspace, createdLabel: formatDate(workspace.createdAt, dateLocale(locale)) }))} />
    </>
  );
}
