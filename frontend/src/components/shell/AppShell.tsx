"use client";

import { usePathname } from "next/navigation";
import { useEffect, useMemo, useState, type ReactNode } from "react";
import { useT } from "@/i18n/client";
import { Sidebar, type NavKey, type WorkspaceRef } from "./Sidebar";
import { Topbar, type Crumb } from "./Topbar";
import styles from "./shell.module.css";

const COLLAPSE_KEY = "prumo_sidebar_collapsed";
const WORKSPACE_PATH = /^\/workspaces\/([^/]+)(?:\/([^/]+))?/;

type AppShellProps = {
  user: { name: string; email: string; avatarUrl?: string | null };
  workspaces: WorkspaceRef[];
  children: ReactNode;
};

export function AppShell({ user, workspaces, children }: AppShellProps) {
  const t = useT();
  const pathname = usePathname();
  const [collapsed, setCollapsed] = useState(false);
  const [menuOpen, setMenuOpen] = useState(false);

  const { workspace, active, crumbs, scroll } = useMemo(() => {
    const match = WORKSPACE_PATH.exec(pathname);

    if (!match) {
      const settings = pathname.startsWith("/settings");

      return {
        workspace: null,
        active: (settings ? "settings" : "home") as NavKey,
        crumbs: [{ label: settings ? t("settings.title") : t("nav.workspaces") }] as Crumb[],
        scroll: true,
      };
    }

    const found = workspaces.find((candidate) => candidate.id === match[1]) ?? null;
    const section = match[2];
    /* A subtask opens from the board, so it keeps the board entry highlighted (but, unlike the board, its page scrolls). */
    const key: NavKey =
      section === "board" || section === "backlog" || section === "repositories"
        ? section
        : section === "subtasks"
          ? "board"
          : "workspaceSettings";
    const title = t(`nav.${key}`);

    return {
      workspace: found,
      active: key,
      crumbs: [
        { label: t("nav.workspaces"), href: "/" },
        ...(found ? [{ label: found.name, href: `/workspaces/${found.id}/board` }] : []),
        { label: title },
      ] as Crumb[],
      scroll: section !== "board",
    };
  }, [pathname, workspaces, t]);

  useEffect(() => {
    try {
      setCollapsed(localStorage.getItem(COLLAPSE_KEY) === "1");
    } catch {
      /* storage unavailable: start expanded */
    }
  }, []);

  useEffect(() => {
    setMenuOpen(false);
  }, [pathname]);

  useEffect(() => {
    if (!menuOpen) return;

    function onKey(event: KeyboardEvent) {
      if (event.key === "Escape") setMenuOpen(false);
    }

    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [menuOpen]);

  function toggle() {
    setCollapsed((current) => {
      try {
        localStorage.setItem(COLLAPSE_KEY, current ? "0" : "1");
      } catch {
        /* the toggle still works for this visit */
      }
      return !current;
    });
  }

  return (
    <div className={`${styles.app} ${collapsed ? styles.collapsed : ""} ${menuOpen ? styles.menuOpen : ""} ${styles.appFill}`}>
      <Topbar user={user} workspaceId={workspace?.id} crumbs={crumbs} onOpenMenu={() => setMenuOpen(true)} />
      <div className={styles.body}>
        <Sidebar
          workspaces={workspaces}
          workspace={workspace}
          active={active}
          collapsed={collapsed}
          onToggle={toggle}
          onNavigate={() => setMenuOpen(false)}
        />
        <button type="button" className={styles.scrim} tabIndex={-1} aria-hidden="true" onClick={() => setMenuOpen(false)} />
        <main className={`${styles.main} ${styles.mainFill} ${scroll ? styles.mainScroll : ""}`}>{children}</main>
      </div>
    </div>
  );
}
