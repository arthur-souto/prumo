"use client";

import { ChevronRight, Menu, Plus, Search } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { useT } from "@/i18n/client";
import { UserMenu } from "./UserMenu";
import styles from "./shell.module.css";

export type Crumb = { label: string; href?: string };

type TopbarProps = {
  user: { name: string; email: string };
  workspaceId?: string;
  crumbs: Crumb[];
  onOpenMenu: () => void;
};

function isTyping(target: EventTarget | null) {
  const element = target as HTMLElement | null;
  return Boolean(element && (element.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(element.tagName)));
}

export function Topbar({ user, workspaceId, crumbs, onOpenMenu }: TopbarProps) {
  const t = useT();
  const router = useRouter();
  const pathname = usePathname();
  const input = useRef<HTMLInputElement>(null);
  const [query, setQuery] = useState("");

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === "/" && !event.metaKey && !event.ctrlKey && !isTyping(event.target)) {
        event.preventDefault();
        input.current?.focus();
      }
    }

    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, []);

  function search(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const term = query.trim();
    const suffix = term ? `?q=${encodeURIComponent(term)}` : "";

    if (!workspaceId) {
      router.push(`/${suffix}`);
      return;
    }

    const target = pathname.endsWith("/backlog") ? "backlog" : "board";
    router.push(`/workspaces/${workspaceId}/${target}${suffix}`);
  }

  function create() {
    router.push(workspaceId ? `/workspaces/${workspaceId}/board?create=1` : "/?create=1");
  }

  return (
    <header className={styles.topbar}>
      <div className={styles.topbarLeft}>
        <span className={styles.menuButton}>
          <IconButton label={t("sidebar.menuOpen")} icon={<Menu size={18} aria-hidden="true" />} onClick={onOpenMenu} />
        </span>
        <nav className={styles.crumbs} aria-label={t("workspace.breadcrumb")}>
          <ol>
            {crumbs.map((crumb, index) => (
              <li key={`${crumb.label}-${index}`}>
                {index > 0 ? <ChevronRight size={14} aria-hidden="true" className={styles.crumbSeparator} /> : null}
                {crumb.href && index < crumbs.length - 1 ? (
                  <Link href={crumb.href}>{crumb.label}</Link>
                ) : (
                  <span aria-current={index === crumbs.length - 1 ? "page" : undefined}>{crumb.label}</span>
                )}
              </li>
            ))}
          </ol>
        </nav>
      </div>

      <form className={styles.search} role="search" onSubmit={search}>
        <Search size={16} aria-hidden="true" className={styles.searchIcon} />
        <input
          ref={input}
          type="search"
          value={query}
          placeholder={t("topbar.searchPlaceholder")}
          aria-label={t("topbar.searchLabel")}
          onChange={(event) => setQuery(event.target.value)}
        />
        <kbd className={styles.kbd} aria-hidden="true">
          /
        </kbd>
      </form>

      <div className={styles.topbarRight}>
        <Button appearance="primary" size="sm" icon={<Plus size={16} aria-hidden="true" />} onClick={create}>
          {t("topbar.create")}
        </Button>
        <UserMenu name={user.name} email={user.email} />
      </div>
    </header>
  );
}
