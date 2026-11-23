"use client";

import Link from "next/link";
import { useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Lozenge } from "@/components/ui/Lozenge";
import { WorkspaceIcon } from "@/components/ui/WorkspaceIcon";
import { useT } from "@/i18n/client";
import type { Workspace } from "@/lib/workspace-types";
import { CreateWorkspaceModal } from "./CreateWorkspaceModal";
import styles from "./workspaces.module.css";

export type WorkspaceRow = Workspace & { createdLabel: string };

export function WorkspacesView({ workspaces }: { workspaces: WorkspaceRow[] }) {
  const t = useT();
  const [query, setQuery] = useState("");
  const [creating, setCreating] = useState(false);

  const visible = useMemo(() => {
    const term = query.trim().toLowerCase();
    return term
      ? workspaces.filter((workspace) => `${workspace.name} ${workspace.slug}`.toLowerCase().includes(term))
      : workspaces;
  }, [workspaces, query]);

  return (
    <main className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{t("workspaces.title")}</h1>
        <Button appearance="primary" onClick={() => setCreating(true)}>
          {t("workspaces.create")}
        </Button>
      </header>

      {workspaces.length === 0 ? (
        <section className={styles.empty}>
          <WorkspaceIcon id="prumo" name="P" size="lg" />
          <h2 className={styles.emptyTitle}>{t("workspaces.emptyTitle")}</h2>
          <p className={styles.emptyText}>{t("workspaces.emptyText")}</p>
          <Button appearance="primary" onClick={() => setCreating(true)}>
            {t("workspaces.create")}
          </Button>
        </section>
      ) : (
        <>
          <div className={styles.toolbar}>
            <input
              className={styles.search}
              type="search"
              placeholder={t("workspaces.searchPlaceholder")}
              aria-label={t("workspaces.searchLabel")}
              value={query}
              onChange={(event) => setQuery(event.target.value)}
            />
          </div>

          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th scope="col">{t("workspaces.columns.name")}</th>
                  <th scope="col">{t("workspaces.columns.slug")}</th>
                  <th scope="col">{t("workspaces.columns.role")}</th>
                  <th scope="col" className={styles.numeric}>
                    {t("workspaces.columns.members")}
                  </th>
                  <th scope="col">{t("workspaces.columns.created")}</th>
                </tr>
              </thead>
              <tbody>
                {visible.map((workspace) => (
                  <tr key={workspace.id}>
                    <td>
                      <Link href={`/workspaces/${workspace.id}`} className={styles.nameLink}>
                        <WorkspaceIcon id={workspace.id} name={workspace.name} />
                        <span>{workspace.name}</span>
                      </Link>
                    </td>
                    <td className={styles.muted}>{workspace.slug}</td>
                    <td>
                      <Lozenge tone={workspace.role === "OWNER" ? "blue" : "neutral"}>{t(`workspaces.roles.${workspace.role}`)}</Lozenge>
                    </td>
                    <td className={styles.numeric}>{workspace.memberCount}</td>
                    <td className={styles.muted}>{workspace.createdLabel}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            {visible.length === 0 ? <p className={styles.noMatch}>{t("workspaces.noMatch", { query })}</p> : null}
          </div>
        </>
      )}

      <CreateWorkspaceModal open={creating} onClose={() => setCreating(false)} />
    </main>
  );
}
