import type { ReactNode } from "react";
import { getT } from "@/i18n/server";
import styles from "./workspace.module.css";

export type WorkspaceSection = "board" | "backlog" | "sprints" | "repositories" | "workspaceSettings";

const TITLES = {
  board: "nav.board",
  backlog: "nav.backlog",
  sprints: "nav.sprints",
  repositories: "nav.repositories",
  workspaceSettings: "nav.workspaceSettings",
} as const;

/** Page body for a workspace section. The top bar and sidebar come from the (app) layout. */
export async function WorkspaceShell({ active, children }: { active: WorkspaceSection; children: ReactNode }) {
  const { t } = await getT();
  const title = t(TITLES[active]);

  return (
    <div className={`${styles.content} ${active === "board" ? styles.contentFill : ""}`}>
      <h1 className={styles.pageTitle}>{title}</h1>
      {children}
    </div>
  );
}
