import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Lozenge } from "@/components/ui/Lozenge";
import { WorkspaceIcon } from "@/components/ui/WorkspaceIcon";
import { requireUser } from "@/lib/session";
import { getWorkspace } from "@/lib/workspaces";
import { getT } from "@/i18n/server";
import { dateLocale } from "@/i18n/core";
import { formatDate } from "@/lib/workspace-types";
import { TopNav } from "../../TopNav";
import { WorkspaceSettings } from "./WorkspaceSettings";
import styles from "./workspace.module.css";

type WorkspacePageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ refreshed?: string }>;
};

export async function generateMetadata({ params }: WorkspacePageProps): Promise<Metadata> {
  const workspace = await getWorkspace((await params).id);
  const { t } = await getT();
  return { title: workspace?.name ?? t("meta.workspace") };
}

export default async function WorkspacePage({ params, searchParams }: WorkspacePageProps) {
  const { id } = await params;
  const { refreshed } = await searchParams;
  const user = await requireUser(`/workspaces/${id}`, refreshed);
  const workspace = await getWorkspace(id);
  const { t, locale } = await getT();

  if (!workspace) {
    notFound();
  }

  return (
    <>
      <TopNav name={user.name} email={user.email} />
      <main className={styles.page}>
        <nav className={styles.breadcrumb} aria-label={t("workspace.breadcrumb")}>
          <Link href="/">{t("workspaces.title")}</Link>
          <span aria-hidden="true">/</span>
          <span>{workspace.name}</span>
        </nav>

        <header className={styles.header}>
          <WorkspaceIcon id={workspace.id} name={workspace.name} size="lg" />
          <div>
            <h1 className={styles.title}>{workspace.name}</h1>
            <p className={styles.slug}>{workspace.slug}</p>
          </div>
        </header>

        <div className={styles.grid}>
          <section className={styles.card}>
            <h2 className={styles.cardTitle}>{t("workspace.details")}</h2>
            <dl className={styles.list}>
              <dt>{t("workspace.description")}</dt>
              <dd>{workspace.description ?? <span className={styles.muted}>{t("workspace.noDescription")}</span>}</dd>
              <dt>{t("workspace.yourRole")}</dt>
              <dd>
                <Lozenge tone={workspace.role === "OWNER" ? "blue" : "neutral"}>{t(`workspaces.roles.${workspace.role}`)}</Lozenge>
              </dd>
              <dt>{t("workspace.members")}</dt>
              <dd>{workspace.memberCount}</dd>
              <dt>{t("workspace.created")}</dt>
              <dd>{formatDate(workspace.createdAt, dateLocale(locale))}</dd>
            </dl>
          </section>

          <WorkspaceSettings
            id={workspace.id}
            name={workspace.name}
            description={workspace.description}
            canEdit={workspace.role === "OWNER" || workspace.role === "ADMIN"}
            canDelete={workspace.role === "OWNER"}
          />
        </div>
      </main>
    </>
  );
}
