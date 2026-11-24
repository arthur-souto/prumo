"use client";

import {
  ChevronsUpDown,
  Columns3,
  GitBranch,
  LayoutGrid,
  ListChecks,
  PanelLeftClose,
  PanelLeftOpen,
  Plus,
  Settings,
  SlidersHorizontal,
} from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import type { ReactNode } from "react";
import {
  DropdownContent,
  DropdownItem,
  DropdownLabel,
  DropdownMenu,
  DropdownSeparator,
  DropdownTrigger,
} from "@/components/ui/DropdownMenu";
import { IconButton } from "@/components/ui/IconButton";
import { Tooltip } from "@/components/ui/Tooltip";
import { WorkspaceIcon } from "@/components/ui/WorkspaceIcon";
import { useT } from "@/i18n/client";
import styles from "./shell.module.css";

export type NavKey = "home" | "settings" | "board" | "backlog" | "repositories" | "workspaceSettings";
export type WorkspaceRef = { id: string; name: string };

type SidebarProps = {
  workspaces: WorkspaceRef[];
  workspace?: WorkspaceRef | null;
  active: NavKey;
  collapsed: boolean;
  onToggle: () => void;
  onNavigate: () => void;
};

type Item = { key: NavKey; href: string; label: string; icon: ReactNode };

export function Sidebar({ workspaces, workspace, active, collapsed, onToggle, onNavigate }: SidebarProps) {
  const t = useT();
  const router = useRouter();
  const size = 18;

  const items: Item[] = workspace
    ? [
        { key: "board", href: `/workspaces/${workspace.id}/board`, label: t("nav.board"), icon: <Columns3 size={size} aria-hidden="true" /> },
        { key: "backlog", href: `/workspaces/${workspace.id}/backlog`, label: t("nav.backlog"), icon: <ListChecks size={size} aria-hidden="true" /> },
        { key: "repositories", href: `/workspaces/${workspace.id}/repositories`, label: t("nav.repositories"), icon: <GitBranch size={size} aria-hidden="true" /> },
        { key: "workspaceSettings", href: `/workspaces/${workspace.id}`, label: t("nav.workspaceSettings"), icon: <SlidersHorizontal size={size} aria-hidden="true" /> },
      ]
    : [
        { key: "home", href: "/", label: t("nav.workspaces"), icon: <LayoutGrid size={size} aria-hidden="true" /> },
        { key: "settings", href: "/settings", label: t("nav.settings"), icon: <Settings size={size} aria-hidden="true" /> },
      ];

  return (
    <nav className={styles.sidebar} aria-label={t("sidebar.label")}>
      <DropdownMenu>
        <DropdownTrigger>
          <button type="button" className={styles.switcher} aria-label={t("sidebar.switchWorkspace")}>
            {workspace ? <WorkspaceIcon id={workspace.id} name={workspace.name} size="sm" /> : <span className={styles.switcherIcon}><LayoutGrid size={18} aria-hidden="true" /></span>}
            <span className={styles.label}>
              <span className={styles.switcherName}>{workspace?.name ?? t("sidebar.selectWorkspace")}</span>
              <span className={styles.switcherKind}>{t("sidebar.workspace")}</span>
            </span>
            <ChevronsUpDown size={16} aria-hidden="true" className={`${styles.label} ${styles.switcherChevron}`} />
          </button>
        </DropdownTrigger>
        <DropdownContent align="start">
          <DropdownLabel>{t("sidebar.yourWorkspaces")}</DropdownLabel>
          {workspaces.map((candidate) => (
            <DropdownItem
              key={candidate.id}
              icon={<WorkspaceIcon id={candidate.id} name={candidate.name} size="sm" />}
              onSelect={() => router.push(`/workspaces/${candidate.id}/board`)}
            >
              {candidate.name}
            </DropdownItem>
          ))}
          <DropdownSeparator />
          <DropdownItem icon={<LayoutGrid size={16} aria-hidden="true" />} onSelect={() => router.push("/")}>
            {t("sidebar.allWorkspaces")}
          </DropdownItem>
          <DropdownItem icon={<Plus size={16} aria-hidden="true" />} onSelect={() => router.push("/?create=1")}>
            {t("sidebar.createWorkspace")}
          </DropdownItem>
        </DropdownContent>
      </DropdownMenu>

      <ul className={styles.items}>
        {items.map((item) => {
          const link = (
            <Link
              href={item.href}
              className={styles.item}
              aria-current={active === item.key ? "page" : undefined}
              aria-label={collapsed ? item.label : undefined}
              onClick={onNavigate}
            >
              {item.icon}
              <span className={styles.label}>{item.label}</span>
            </Link>
          );

          return (
            <li key={item.key}>
              {collapsed ? (
                <Tooltip content={item.label} side="right">
                  {link}
                </Tooltip>
              ) : (
                link
              )}
            </li>
          );
        })}
      </ul>

      <div className={styles.sidebarFooter}>
        <span className={styles.collapseButton}>
          <IconButton
            size="sm"
            label={collapsed ? t("sidebar.expand") : t("sidebar.collapse")}
            icon={collapsed ? <PanelLeftOpen size={18} aria-hidden="true" /> : <PanelLeftClose size={18} aria-hidden="true" />}
            onClick={onToggle}
            aria-expanded={!collapsed}
          />
        </span>
      </div>
    </nav>
  );
}
