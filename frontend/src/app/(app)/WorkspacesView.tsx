"use client";

import { Plus, Search, Users } from "lucide-react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Lozenge } from "@/components/ui/Lozenge";
import { WorkspaceIcon } from "@/components/ui/WorkspaceIcon";
import { useT } from "@/i18n/client";
import type { Workspace } from "@/lib/workspace-types";
import { CreateWorkspaceModal } from "./CreateWorkspaceModal";
import styles from "./workspaces.module.css";

export type WorkspaceRow = Workspace & { createdLabel: string; total: number; done: number };

export function WorkspacesView({ workspaces }: { workspaces: WorkspaceRow[] }) {
  const t = useT();
  const router = useRouter();
  const params = useSearchParams();
  const [query, setQuery] = useState(params.get("q") ?? "");
  const [creating, setCreating] = useState(false);

  useEffect(() => {
    setQuery(params.get("q") ?? "");

    if (params.get("create") === "1") {
      setCreating(true);
      router.replace("/");
    }
  }, [params, router]);

  const visible = useMemo(() => {
    const term = query.trim().toLowerCase();
    return term
      ? workspaces.filter((workspace) => `${workspace.name} ${workspace.slug}`.toLowerCase().includes(term))
      : workspaces;
  }, [workspaces, query]);

  return (
    <div className={styles.page}>
      <header className={styles.header}>
        <h1 className={styles.title}>{t("workspaces.title")}</h1>
        <Button appearance="primary" icon={<Plus size={16} aria-hidden="true" />} onClick={() => setCreating(true)}>
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
            <span className={styles.searchWrap}>
              <Search size={16} aria-hidden="true" className={styles.searchIcon} />
              <input
                className={styles.search}
                type="search"
                placeholder={t("workspaces.searchPlaceholder")}
                aria-label={t("workspaces.searchLabel")}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </span>
          </div>

          <ul className={styles.cards}>
            {visible.map((workspace) => {
              const percent = workspace.total === 0 ? 0 : Math.round((workspace.done / workspace.total) * 100);

              return (
                <li key={workspace.id}>
                  <Link href={`/workspaces/${workspace.id}/board`} className={styles.card}>
                    <span className={styles.cardHead}>
                      <WorkspaceIcon id={workspace.id} name={workspace.name} />
                      <span className={styles.cardName}>{workspace.name}</span>
                      <Lozenge tone={workspace.role === "OWNER" ? "blue" : "neutral"}>{t(`workspaces.roles.${workspace.role}`)}</Lozenge>
                    </span>
                    <span className={styles.progressLabel}>
                      {workspace.total === 0 ? t("workspaces.noStories") : t("workspaces.progress", { done: workspace.done, total: workspace.total })}
                    </span>
                    <span
                      className={styles.progress}
                      role="progressbar"
                      aria-label={t("workspaces.progressLabel", { name: workspace.name })}
                      aria-valuemin={0}
                      aria-valuemax={100}
                      aria-valuenow={percent}
                    >
                      <span style={{ width: `${percent}%` }} />
                    </span>
                    <span className={styles.cardFoot}>
                      <Users size={14} aria-hidden="true" />
                      {workspace.memberCount}
                      <span className={styles.muted}>· {workspace.createdLabel}</span>
                    </span>
                  </Link>
                </li>
              );
            })}
          </ul>
          {visible.length === 0 ? <p className={styles.noMatch}>{t("workspaces.noMatch", { query })}</p> : null}
        </>
      )}

      <CreateWorkspaceModal open={creating} onClose={() => setCreating(false)} />
    </div>
  );
}
