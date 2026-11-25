"use client";

import { ChevronRight, Menu, Plus, Search } from "lucide-react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { StorySuggestions, useStorySuggestions } from "@/components/search/StorySuggestions";
import { Button } from "@/components/ui/Button";
import { IconButton } from "@/components/ui/IconButton";
import { useT } from "@/i18n/client";
import type { Sprint, Story } from "@/lib/board-types";
import { replaceSearchParams } from "@/lib/url-state";
import { UserMenu } from "./UserMenu";
import styles from "./shell.module.css";

export type Crumb = { label: string; href?: string };

type TopbarProps = {
  user: { name: string; email: string; avatarUrl?: string | null };
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
  const target = pathname.endsWith("/backlog") ? "backlog" : pathname.endsWith("/sprints") ? "sprints" : "board";
  const suggestions = useStorySuggestions(workspaceId, query, workspaceId ? openStory : undefined, workspaceId ? openSprint : undefined);

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

    const path = `/workspaces/${workspaceId}/${target}`;

    if (pathname === path) {
      replaceSearchParams((current) => {
        if (term) current.set("q", term);
        else current.delete("q");
        current.delete("page");
      });
      return;
    }

    router.push(`${path}${suffix}`);
  }

  function openSprint(sprint: Sprint) {
    setQuery("");
    input.current?.blur();
    const path = `/workspaces/${workspaceId}/sprints`;

    if (pathname === path) {
      replaceSearchParams((current) => current.set("sprint", sprint.id));
      return;
    }

    router.push(`${path}?sprint=${encodeURIComponent(sprint.id)}`);
  }

  function openStory(story: Story) {
    setQuery("");
    input.current?.blur();
    const path = `/workspaces/${workspaceId}/${target === "sprints" ? "board" : target}`;

    if (pathname === path) {
      replaceSearchParams((current) => current.set("story", story.key));
      return;
    }

    router.push(`${path}?story=${encodeURIComponent(story.key)}`);
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
          {...suggestions.inputProps}
          onChange={(event) => {
            setQuery(event.target.value);
            suggestions.reopen();
          }}
        />
        <kbd className={styles.kbd} aria-hidden="true">
          /
        </kbd>
        <StorySuggestions {...suggestions.list} />
      </form>

      <div className={styles.topbarRight}>
        <Button appearance="primary" size="sm" icon={<Plus size={16} aria-hidden="true" />} onClick={create}>
          {t("topbar.create")}
        </Button>
        <UserMenu name={user.name} email={user.email} avatarUrl={user.avatarUrl} />
      </div>
    </header>
  );
}
