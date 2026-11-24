"use client";

import { ListChecks, Plus, Trash2, X } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { Select } from "@/components/ui/Select";
import { useToast } from "@/components/ui/Toast";
import { errorMessage, useT } from "@/i18n/client";
import { PRIORITIES, type Board, type Priority, type Repository, type Story } from "@/lib/board-types";
import { EMPTY_FILTERS, allLabels, hasFilters, matchesFilters, type Filters } from "@/lib/board-utils";
import { sendJson } from "@/lib/client";
import { StoryDrawer } from "../board/StoryDrawer";
import styles from "./backlog.module.css";

type BacklogViewProps = {
  workspaceId: string;
  board: Board;
  repositories: Repository[];
  hasGithubToken: boolean;
};

export function BacklogView({ workspaceId, board, repositories: initialRepositories, hasGithubToken }: BacklogViewProps) {
  const t = useT();
  const toast = useToast();
  const base = `/api/workspaces/${workspaceId}`;
  const params = useSearchParams();
  const [stories, setStories] = useState<Record<string, Story>>(() => Object.fromEntries(board.stories.map((story) => [story.id, story])));
  const [repositories, setRepositories] = useState(initialRepositories);
  const [filters, setFilters] = useState<Filters>({ ...EMPTY_FILTERS, text: params.get("q") ?? "" });
  const [status, setStatus] = useState("");
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [busyIds, setBusyIds] = useState<Set<string>>(new Set());
  const [openId, setOpenId] = useState<string | null>(null);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [bulkBusy, setBulkBusy] = useState(false);
  const selectAll = useRef<HTMLInputElement>(null);

  useEffect(() => {
    setFilters((current) => ({ ...current, text: params.get("q") ?? "" }));
  }, [params]);

  const columns = useMemo(() => [...board.columns].sort((a, b) => a.position - b.position), [board.columns]);
  const columnById = useMemo(() => new Map(columns.map((column) => [column.id, column])), [columns]);
  const labels = useMemo(() => allLabels(Object.values(stories)), [stories]);

  const rows = useMemo(() => {
    const order = new Map(columns.map((column) => [column.id, column.position]));

    return Object.values(stories)
      .filter((story) => matchesFilters(story, filters) && (status === "" || story.columnId === status))
      .sort((a, b) => (order.get(a.columnId) ?? 0) - (order.get(b.columnId) ?? 0) || b.createdAt.localeCompare(a.createdAt));
  }, [stories, filters, status, columns]);

  const visibleIds = useMemo(() => new Set(rows.map((story) => story.id)), [rows]);
  const chosen = useMemo(() => [...selected].filter((id) => visibleIds.has(id)), [selected, visibleIds]);
  const allChecked = rows.length > 0 && chosen.length === rows.length;
  const filtering = hasFilters(filters) || status !== "";

  useEffect(() => {
    if (selectAll.current) {
      selectAll.current.indeterminate = chosen.length > 0 && !allChecked;
    }
  }, [chosen.length, allChecked]);

  const upsert = useCallback((story: Story) => setStories((current) => ({ ...current, [story.id]: story })), []);

  const reloadRepositories = useCallback(async () => {
    const result = await sendJson<Repository[]>(`${base}/repositories`, "GET");

    if (result.ok && result.data) setRepositories(result.data);
  }, [base]);

  async function moveToColumn(story: Story, columnId: string) {
    const result = await sendJson<Story>(`${base}/stories/${story.key}/move`, "PUT", { columnId });

    if (!result.ok || !result.data) {
      toast({ tone: "danger", title: errorMessage(t, result.code, result.reason) });
      return false;
    }

    upsert(result.data);
    return true;
  }

  async function changePriority(story: Story, priority: Priority) {
    // PUT replaces the whole story, so the untouched fields are sent back as they are.
    const result = await sendJson<Story>(`${base}/stories/${story.key}`, "PUT", {
      title: story.title,
      description: story.description ?? "",
      priority,
      storyPoints: story.storyPoints,
      labels: story.labels,
      subtasks: story.subtasks,
      acceptanceCriteria: story.acceptanceCriteria,
    });

    if (!result.ok || !result.data) {
      toast({ tone: "danger", title: errorMessage(t, result.code, result.reason) });
      return false;
    }

    upsert(result.data);
    return true;
  }

  async function inline(story: Story, action: () => Promise<boolean>) {
    setBusyIds((current) => new Set(current).add(story.id));
    await action();
    setBusyIds((current) => {
      const next = new Set(current);
      next.delete(story.id);
      return next;
    });
  }

  async function bulk(action: (story: Story) => Promise<boolean>) {
    const targets = chosen.map((id) => stories[id]).filter(Boolean);
    setBulkBusy(true);
    const results = await Promise.all(targets.map(action));
    setBulkBusy(false);

    const failed = results.filter((ok) => !ok).length;

    if (failed === 0) {
      toast({ tone: "success", title: t("backlog.updated", { count: targets.length }) });
    } else if (failed < targets.length) {
      toast({ tone: "danger", title: t("backlog.partial", { failed, count: targets.length }) });
    }
  }

  async function bulkDelete() {
    const targets = chosen.map((id) => stories[id]).filter(Boolean);
    setBulkBusy(true);
    const results = await Promise.all(
      targets.map(async (story) => {
        const result = await sendJson(`${base}/stories/${story.key}`, "DELETE");
        return result.ok ? story.id : null;
      }),
    );
    setBulkBusy(false);
    setConfirmingDelete(false);

    const removed = new Set(results.filter((id): id is string => id !== null));
    setStories((current) => Object.fromEntries(Object.entries(current).filter(([id]) => !removed.has(id))));
    setSelected((current) => new Set([...current].filter((id) => !removed.has(id))));

    if (removed.size === targets.length) {
      toast({ tone: "success", title: t("backlog.deleted", { count: removed.size }) });
    } else {
      toast({ tone: "danger", title: t("backlog.partial", { failed: targets.length - removed.size, count: targets.length }) });
    }
  }

  function toggle(id: string, on: boolean) {
    setSelected((current) => {
      const next = new Set(current);
      if (on) next.add(id);
      else next.delete(id);
      return next;
    });
  }

  const open = openId ? stories[openId] : undefined;
  const total = Object.keys(stories).length;

  if (total === 0) {
    return (
      <EmptyState
        icon={<ListChecks size={20} aria-hidden="true" />}
        title={t("backlog.emptyTitle")}
        description={t("backlog.emptyText")}
        action={
          <Link href={`/workspaces/${workspaceId}/board?create=1`}>
            <Button appearance="primary" icon={<Plus size={16} aria-hidden="true" />} tabIndex={-1}>
              {t("board.createStory")}
            </Button>
          </Link>
        }
      />
    );
  }

  return (
    <div className={styles.view}>
      <div className={styles.filters}>
        <Select compact variant="quiet" value={status} aria-label={t("backlog.status")} onChange={(event) => setStatus(event.target.value)}>
          <option value="">{t("backlog.allStatuses")}</option>
          {columns.map((column) => (
            <option key={column.id} value={column.id}>
              {column.name}
            </option>
          ))}
        </Select>
        <Select
          compact
          variant="quiet"
          value={filters.priority}
          aria-label={t("backlog.priority")}
          onChange={(event) => setFilters({ ...filters, priority: event.target.value as Priority | "" })}
        >
          <option value="">{t("board.allPriorities")}</option>
          {PRIORITIES.map((value) => (
            <option key={value} value={value}>
              {t(`priority.${value}`)}
            </option>
          ))}
        </Select>
        <Select compact variant="quiet" value={filters.label} aria-label={t("story.labels")} onChange={(event) => setFilters({ ...filters, label: event.target.value })}>
          <option value="">{t("board.allLabels")}</option>
          {labels.map((label) => (
            <option key={label} value={label}>
              {label}
            </option>
          ))}
        </Select>
        {filtering ? (
          <Button
            appearance="subtle"
            size="sm"
            icon={<X size={14} aria-hidden="true" />}
            onClick={() => {
              setFilters(EMPTY_FILTERS);
              setStatus("");
            }}
          >
            {t("board.clearFilters")}
          </Button>
        ) : null}
        <span className={styles.count} role="status">
          {t("backlog.showing", { shown: rows.length, total })}
        </span>
      </div>

      {rows.length === 0 ? (
        <EmptyState icon={<ListChecks size={20} aria-hidden="true" />} title={filters.text.trim() ? t("backlog.noMatch", { query: filters.text.trim() }) : t("board.noMatchColumn")} compact />
      ) : (
        <div className={styles.wrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th scope="col" className={styles.check}>
                  <input
                    ref={selectAll}
                    type="checkbox"
                    checked={allChecked}
                    aria-label={t("backlog.selectAll")}
                    onChange={(event) => setSelected(event.target.checked ? new Set(rows.map((story) => story.id)) : new Set())}
                  />
                </th>
                <th scope="col">{t("backlog.key")}</th>
                <th scope="col">{t("backlog.title")}</th>
                <th scope="col">{t("backlog.status")}</th>
                <th scope="col">{t("backlog.priority")}</th>
                <th scope="col" className={styles.numeric}>
                  {t("backlog.points")}
                </th>
              </tr>
            </thead>
            <tbody>
              {rows.map((story) => {
                const checked = selected.has(story.id);
                const busy = busyIds.has(story.id) || bulkBusy;

                return (
                  <tr key={story.id} className={checked ? styles.selected : undefined}>
                    <td className={styles.check}>
                      <input
                        type="checkbox"
                        checked={checked}
                        aria-label={t("backlog.selectRow", { key: story.key })}
                        onChange={(event) => toggle(story.id, event.target.checked)}
                      />
                    </td>
                    <td className={styles.key}>{story.key}</td>
                    <td className={styles.titleCell}>
                      <button type="button" className={styles.titleButton} onClick={() => setOpenId(story.id)}>
                        {story.title}
                      </button>
                      {story.labels.length > 0 ? (
                        <span className={styles.labels}>
                          {story.labels.map((label) => (
                            <span key={label} className={styles.label}>
                              {label}
                            </span>
                          ))}
                        </span>
                      ) : null}
                    </td>
                    <td>
                      <Select
                        compact
                        variant="quiet"
                        value={story.columnId}
                        disabled={busy}
                        aria-label={`${t("backlog.status")}: ${story.key}`}
                        onChange={(event) => void inline(story, () => moveToColumn(story, event.target.value))}
                      >
                        {columns.map((column) => (
                          <option key={column.id} value={column.id}>
                            {column.name}
                          </option>
                        ))}
                      </Select>
                    </td>
                    <td>
                      <Select
                        compact
                        variant="quiet"
                        value={story.priority}
                        disabled={busy}
                        aria-label={`${t("backlog.priority")}: ${story.key}`}
                        onChange={(event) => void inline(story, () => changePriority(story, event.target.value as Priority))}
                      >
                        {PRIORITIES.map((value) => (
                          <option key={value} value={value}>
                            {t(`priority.${value}`)}
                          </option>
                        ))}
                      </Select>
                    </td>
                    <td className={styles.numeric}>{story.storyPoints ?? ""}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {chosen.length > 0 ? (
        <div className={styles.bulkBar} role="region" aria-label={t("backlog.bulkLabel")}>
          <span className={styles.bulkCount}>{t("backlog.selected", { count: chosen.length })}</span>
          <Select
            compact
            value=""
            disabled={bulkBusy}
            aria-label={t("backlog.moveTo")}
            onChange={(event) => {
              const columnId = event.target.value;
              if (columnId) void bulk((story) => moveToColumn(story, columnId));
            }}
          >
            <option value="">{t("backlog.moveTo")}</option>
            {columns.map((column) => (
              <option key={column.id} value={column.id}>
                {column.name}
              </option>
            ))}
          </Select>
          <Select
            compact
            value=""
            disabled={bulkBusy}
            aria-label={t("backlog.setPriority")}
            onChange={(event) => {
              const priority = event.target.value as Priority;
              if (priority) void bulk((story) => changePriority(story, priority));
            }}
          >
            <option value="">{t("backlog.setPriority")}</option>
            {PRIORITIES.map((value) => (
              <option key={value} value={value}>
                {t(`priority.${value}`)}
              </option>
            ))}
          </Select>
          <Button size="sm" appearance="danger" icon={<Trash2 size={14} aria-hidden="true" />} disabled={bulkBusy} onClick={() => setConfirmingDelete(true)}>
            {t("backlog.delete")}
          </Button>
          <Button size="sm" appearance="subtle" onClick={() => setSelected(new Set())}>
            {t("backlog.clearSelection")}
          </Button>
        </div>
      ) : null}

      <ConfirmDialog
        open={confirmingDelete}
        title={t("backlog.deleteTitle", { count: chosen.length })}
        description={t("backlog.deleteLead")}
        confirmLabel={t("backlog.delete")}
        busy={bulkBusy}
        onConfirm={bulkDelete}
        onCancel={() => setConfirmingDelete(false)}
      />

      {open ? (
        <StoryDrawer
          key={open.id}
          workspaceId={workspaceId}
          story={open}
          columns={columns}
          repositories={repositories}
          hasGithubToken={hasGithubToken}
          onChange={upsert}
          onMoveToColumn={moveToColumn}
          onRepositoriesChanged={reloadRepositories}
          onDeleted={() => {
            setStories((current) => Object.fromEntries(Object.entries(current).filter(([id]) => id !== open.id)));
            setOpenId(null);
          }}
          onClose={() => setOpenId(null)}
        />
      ) : null}
    </div>
  );
}
