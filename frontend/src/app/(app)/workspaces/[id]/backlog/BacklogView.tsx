"use client";

import { ChevronLeft, ChevronRight, ListChecks, Plus, Trash2 } from "lucide-react";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { EmptyState } from "@/components/ui/EmptyState";
import { ErrorState } from "@/components/ui/ErrorState";
import { IconButton } from "@/components/ui/IconButton";
import { Select } from "@/components/ui/Select";
import { Skeleton } from "@/components/ui/Skeleton";
import { useToast } from "@/components/ui/Toast";
import { errorMessage, useT } from "@/i18n/client";
import { PRIORITIES, type Board, type Member, type Priority, type Repository, type Story } from "@/lib/board-types";
import { allLabels } from "@/lib/board-utils";
import { sendJson } from "@/lib/client";
import {
  DEFAULT_STORY_ORDER,
  STORY_ORDERS,
  hasStoryFilters,
  readPage,
  readStoryOrder,
  storySearchQuery,
  type StoryOrder,
  type StoryPage,
} from "@/lib/story-filters";
import { replaceSearchParams } from "@/lib/url-state";
import { StoryDrawer } from "../board/StoryDrawer";
import { StoryFilterBar } from "../StoryFilterBar";
import { useStoryFilters } from "../useStoryFilters";
import { useStoryParam } from "../useStoryParam";
import styles from "./backlog.module.css";

const PAGE_SIZE = 25;

type BacklogViewProps = {
  workspaceId: string;
  board: Board;
  repositories: Repository[];
  members: Member[];
  hasGithubToken: boolean;
};

export function BacklogView({ workspaceId, board, repositories: initialRepositories, members, hasGithubToken }: BacklogViewProps) {
  const t = useT();
  const toast = useToast();
  const base = `/api/workspaces/${workspaceId}`;
  const { filters, applied, setFilters } = useStoryFilters();
  const params = useSearchParams();
  const order = readStoryOrder(params);
  const page = readPage(params);
  const [result, setResult] = useState<StoryPage | null>(null);
  const [stories, setStories] = useState<Story[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [repositories, setRepositories] = useState(initialRepositories);
  const [selected, setSelected] = useState<Set<string>>(new Set());
  const [busyIds, setBusyIds] = useState<Set<string>>(new Set());
  const [opened, setOpened] = useState<Story | null>(null);
  const [storyKey, clearStoryKey] = useStoryParam();
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [bulkBusy, setBulkBusy] = useState(false);
  const selectAll = useRef<HTMLInputElement>(null);

  const columns = useMemo(() => [...board.columns].sort((a, b) => a.position - b.position), [board.columns]);
  const labels = useMemo(() => allLabels(board.stories), [board.stories]);
  const query = useMemo(() => storySearchQuery(applied, order, page, PAGE_SIZE), [applied, order, page]);

  useEffect(() => {
    let current = true;
    setLoading(true);
    setFailed(false);

    sendJson<StoryPage>(`${base}/stories?${query}`, "GET").then((response) => {
      if (!current) return;

      setLoading(false);

      if (!response.ok || !response.data) {
        setFailed(true);
        return;
      }

      const data = response.data;

      if (data.items.length === 0 && data.page > 0 && data.totalPages > 0) {
        setPage(data.totalPages - 1);
        return;
      }

      setResult(data);
      setStories(data.items);
      setSelected(new Set());
    });

    return () => {
      current = false;
    };
  }, [base, query, reloadKey]);

  const rows = stories;
  const chosen = useMemo(() => rows.filter((story) => selected.has(story.id)).map((story) => story.id), [rows, selected]);
  const allChecked = rows.length > 0 && chosen.length === rows.length;
  const filtering = hasStoryFilters(applied);
  const reload = useCallback(() => setReloadKey((key) => key + 1), []);

  useEffect(() => {
    if (!storyKey) return;

    let current = true;

    sendJson<Story>(`${base}/stories/${encodeURIComponent(storyKey)}`, "GET").then((response) => {
      if (!current) return;

      if (response.ok && response.data) {
        setOpened(response.data);
      } else {
        toast({ tone: "danger", title: errorMessage(t, response.code, response.reason) });
        clearStoryKey();
      }
    });

    return () => {
      current = false;
    };
  }, [base, storyKey, clearStoryKey, toast, t]);

  function setOrder(next: StoryOrder) {
    replaceSearchParams((current) => {
      if (next === DEFAULT_STORY_ORDER) current.delete("sort");
      else current.set("sort", next);
      current.delete("page");
    });
  }

  function setPage(next: number) {
    replaceSearchParams((current) => {
      if (next > 0) current.set("page", String(next + 1));
      else current.delete("page");
    });
  }

  function closeStory() {
    setOpened(null);
    clearStoryKey();
  }

  useEffect(() => {
    if (selectAll.current) {
      selectAll.current.indeterminate = chosen.length > 0 && !allChecked;
    }
  }, [chosen.length, allChecked]);

  const upsert = useCallback((story: Story) => {
    setStories((current) => current.map((candidate) => (candidate.id === story.id ? story : candidate)));
    setOpened((current) => (current?.id === story.id ? story : current));
  }, []);

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
      subtasks: story.subtasks ?? [],
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
    const targets = rows.filter((story) => selected.has(story.id));
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
    const targets = rows.filter((story) => selected.has(story.id));
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
    setSelected(new Set());
    reload();

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

  const open = opened ? (rows.find((story) => story.id === opened.id) ?? opened) : undefined;

  if (result && result.totalItems === 0 && !filtering && !loading) {
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

  const from = result && result.totalItems > 0 ? result.page * result.size + 1 : 0;
  const to = result ? result.page * result.size + rows.length : 0;

  return (
    <div className={styles.view}>
      <StoryFilterBar filters={filters} onChange={setFilters} labels={labels} members={members} columns={columns} workspaceId={workspaceId} onPick={setOpened}>
        <Select
          compact
          variant="quiet"
          value={order}
          aria-label={t("filters.orderLabel")}
          onChange={(event) => setOrder(event.target.value as StoryOrder)}
        >
          {STORY_ORDERS.map((value) => (
            <option key={value} value={value}>
              {t(`filters.order.${value}`)}
            </option>
          ))}
        </Select>
        {result ? (
          <span className={styles.count} role="status">
            {t("backlog.range", { from, to, total: result.totalItems })}
          </span>
        ) : null}
      </StoryFilterBar>

      {failed ? (
        <ErrorState compact onRetry={reload} />
      ) : !result ? (
        <div className={styles.wrap} aria-busy="true">
          {Array.from({ length: 8 }, (_, index) => (
            <div key={index} className={styles.skeletonRow}>
              <Skeleton width="4rem" />
              <Skeleton width="60%" />
              <Skeleton width="6rem" />
            </div>
          ))}
        </div>
      ) : rows.length === 0 ? (
        <EmptyState icon={<ListChecks size={20} aria-hidden="true" />} title={applied.text ? t("backlog.noMatch", { query: applied.text }) : t("board.noMatchColumn")} compact />
      ) : (
        <div className={`${styles.wrap} ${loading ? styles.stale : ""}`} aria-busy={loading || undefined}>
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
                      <button type="button" className={styles.titleButton} onClick={() => setOpened(story)}>
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

      {result && result.totalPages > 1 ? (
        <nav className={styles.pager} aria-label={t("pagination.label")}>
          <IconButton
            size="sm"
            label={t("pagination.previous")}
            icon={<ChevronLeft size={16} aria-hidden="true" />}
            disabled={page === 0 || loading}
            onClick={() => setPage(page - 1)}
          />
          <span className={styles.pageInfo}>{t("pagination.page", { page: page + 1, pages: result.totalPages })}</span>
          <IconButton
            size="sm"
            label={t("pagination.next")}
            icon={<ChevronRight size={16} aria-hidden="true" />}
            disabled={page + 1 >= result.totalPages || loading}
            onClick={() => setPage(page + 1)}
          />
        </nav>
      ) : null}

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
          members={members}
          workspaceId={workspaceId}
          story={open}
          columns={columns}
          repositories={repositories}
          hasGithubToken={hasGithubToken}
          onChange={upsert}
          onMoveToColumn={moveToColumn}
          onRepositoriesChanged={reloadRepositories}
          onDeleted={() => {
            closeStory();
            reload();
          }}
          onClose={closeStory}
        />
      ) : null}
    </div>
  );
}
