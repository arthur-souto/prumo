import type { Metadata } from "next";
import { requireUser } from "@/lib/session";
import { getGithubConnection } from "@/lib/workspaces";
import { getT } from "@/i18n/server";
import styles from "../workspaces/[id]/workspace.module.css";
import { GithubTokenSettings } from "./GithubTokenSettings";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("meta.settings") };
}

type SettingsPageProps = {
  searchParams: Promise<{ refreshed?: string }>;
};

export default async function SettingsPage({ searchParams }: SettingsPageProps) {
  const { refreshed } = await searchParams;
  await requireUser("/settings", refreshed);
  const connection = await getGithubConnection();
  const { t } = await getT();

  return (
<div className={styles.page}>
        <h1 className={styles.title}>{t("settings.title")}</h1>
        <div className={styles.grid}>
          <GithubTokenSettings connection={connection} />
        </div>
      </div>
  );
}
