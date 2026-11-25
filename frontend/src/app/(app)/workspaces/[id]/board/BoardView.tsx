"use client";

import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragOverEvent,
  type DragStartEvent,
} from "@dnd-kit/core";
import { arrayMove, sortableKeyboardCoordinates } from "@dnd-kit/sortable";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { useToast } from "@/components/ui/Toast";
import { Plus, RefreshCw, Columns3 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { Select } from "@/components/ui/Select";
import { Modal } from "@/components/ui/Modal";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, githubReason, useT } from "@/i18n/client";
import { allLabels, buildLayout, neighbours, type Layout } from "@/lib/board-utils";
import { PRIORITIES, type Board, type Column, type Member, type Priority, type Repository, type Story } from "@/lib/board-types";
import { hasStoryFilters, matchesStoryFilters } from "@/lib/story-filters";
import { sendJson } from "@/lib/client";
import { replaceSearchParams } from "@/lib/url-state";
import modal from "../../../modal-form.module.css";
import settings from "../workspace.module.css";
import { BoardColumn } from "./BoardColumn";
import { CardContent } from "./StoryCard";
import { StoryDrawer } from "./StoryDrawer";
import { StoryFilterBar } from "../StoryFilterBar";
import { useStoryFilters } from "../useStoryFilters";
import { useStoryParam } from "../useStoryParam";
import styles from "./board.module.css";

type BoardViewProps = {
  members: Member[];
  workspaceId: string;
  canManageBoard: boolean;
  initialBoard: Board;
  initialRepositories: Repository[];
  hasGithubToken: boolean;
};

type ColumnDialog =
  | { kind: "create" }
  | { kind: "rename"; column: Column }
  | { kind: "delete"; column: Column }
  | null;

export function BoardView({
  workspaceId,
  canManageBoard,
  initialBoard,
  initialRepositories,
  members,
  hasGithubToken,
}: BoardViewProps) {
  const t = useT();
  const base = `/api/workspaces/${workspaceId}`;
  const [columns, setColumns] = useState(initialBoard.columns);
  const [stories, setStories] = useState<Record<string, Story>>(() =>
    Object.fromEntries(initialBoard.stories.map((story) => [story.id, story])),
  );
  const [layout, setLayout] = useState<Layout>(() => buildLayout(initialBoard));
  const [repositories, setRepositories] = useState(initialRepositories);
  const params = useSearchParams();
  const { filters, setFilters } = useStoryFilters();
  const [activeId, setActiveId] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [storyKey, clearStoryKey] = useStoryParam();
  const [creatingIn, setCreatingIn] = useState<string | null>(null);
  const [dialog, setDialog] = useState<ColumnDialog>(null);
  const toast = useToast();
  const [landedId, setLandedId] = useState<string | null>(null);
  const setError = useCallback(
    (message?: string) => {
      if (message) toast({ tone: "danger", title: message });
    },
    [toast],
  );
  const [syncing, setSyncing] = useState(false);
  const [syncProblems, setSyncProblems] = useState<string[]>([]);
  const origin = useRef<{ column: string; after?: string; before?: string } | null>(null);

  const sensors = useSensors(
    useSensor(PointerSensor, { activationConstraint: { distance: 6 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  useEffect(() => {
    if (params.get("create") === "1") {
      setCreatingIn("first");
      replaceSearchParams((current) => current.delete("create"));
    }
  }, [params]);

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      const target = event.target as HTMLElement | null;
      const typing = Boolean(target && (target.isContentEditable || ["INPUT", "TEXTAREA", "SELECT"].includes(target.tagName)));
      const busy = creatingIn !== null || dialog !== null || selectedId !== null || document.querySelector("dialog[open]");

      if (event.key === "c" && !event.metaKey && !event.ctrlKey && !event.altKey && !typing && !busy) {
        event.preventDefault();
        setCreatingIn("first");
      }
    }

    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [creatingIn, dialog, selectedId]);

  useEffect(() => {
    if (!storyKey) return;

    const found = Object.values(stories).find((story) => story.key.toUpperCase() === storyKey.toUpperCase());

    if (found) {
      setSelectedId(found.id);
    } else {
      toast({ tone: "danger", title: t("errors.notFound") });
      clearStoryKey();
    }
  }, [storyKey, stories, clearStoryKey, toast, t]);

  function closeStory() {
    setSelectedId(null);
    clearStoryKey();
  }

  function openPicked(story: Story) {
    if (!stories[story.id]) upsert(story);
    setSelectedId(story.id);
  }

  const filtering = hasStoryFilters(filters);
  const labels = useMemo(() => allLabels(Object.values(stories)), [stories]);
  const selected = selectedId ? stories[selectedId] : undefined;
  const active = activeId ? stories[activeId] : undefined;

  const reload = useCallback(async () => {
    const [board, repos] = await Promise.all([
      sendJson<Board>(`${base}/board`, "GET"),
      sendJson<Repository[]>(`${base}/repositories`, "GET"),
    ]);

    if (board.ok && board.data) {
      setColumns(board.data.columns);
      setStories(Object.fromEntries(board.data.stories.map((story) => [story.id, story])));
      setLayout(buildLayout(board.data));
    }

    if (repos.ok && repos.data) {
      setRepositories(repos.data);
    }
  }, [base]);

  const upsert = useCallback((story: Story) => {
    setStories((current) => ({ ...current, [story.id]: story }));
  }, []);

  function findColumn(id: string | number | undefined) {
    if (id === undefined) return undefined;
    const key = String(id);
    if (key in layout) return key;
    return Object.keys(layout).find((columnId) => layout[columnId].includes(key));
  }

  function onDragStart(event: DragStartEvent) {
    const id = String(event.active.id);
    const column = findColumn(id);

    setActiveId(id);
    origin.current = column ? { column, ...neighbours(layout[column], id) } : null;
  }

  function onDragOver({ active: dragged, over }: DragOverEvent) {
    const from = findColumn(dragged.id);
    const to = findColumn(over?.id);

    if (!over || !from || !to || from === to) return;

    setLayout((current) => {
      const target = current[to];
      const overIndex = target.indexOf(String(over.id));
      const translated = dragged.rect.current.translated;
      const below = translated ? translated.top > over.rect.top + over.rect.height : false;
      const index = String(over.id) in current ? target.length : overIndex >= 0 ? overIndex + (below ? 1 : 0) : target.length;

      return {
        ...current,
        [from]: current[from].filter((id) => id !== String(dragged.id)),
        [to]: [...target.slice(0, index), String(dragged.id), ...target.slice(index)],
      };
    });
  }

  async function onDragEnd({ active: dragged, over }: DragEndEvent) {
    setActiveId(null);
    const id = String(dragged.id);
    const container = findColumn(id);

    if (!container || !over) return;

    let items = layout[container];
    const overIndex = items.indexOf(String(over.id));
    const activeIndex = items.indexOf(id);

    if (overIndex >= 0 && overIndex !== activeIndex) {
      items = arrayMove(items, activeIndex, overIndex);
      setLayout((current) => ({ ...current, [container]: items }));
    }

    const story = stories[id];
    const { after, before } = neighbours(items, id);
    const start = origin.current;
    const unchanged = start?.column === container && start.after === after && start.before === before;

    if (unchanged) return;

    const result = await sendJson<Story>(`${base}/stories/${story.key}/move`, "PUT", {
      columnId: container,
      afterStoryId: after,
      beforeStoryId: before,
    });

    if (result.ok && result.data) {
      upsert(result.data);
      setLandedId(id);
      window.setTimeout(() => setLandedId((current) => (current === id ? null : current)), 1200);
      return;
    }

    setError(errorMessage(t, result.code, result.reason));
    await reload();
  }

  async function createStory(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const result = await sendJson<Story>(`${base}/stories`, "POST", {
      title: data.get("title"),
      priority: data.get("priority"),
      columnId: creatingIn === "first" ? undefined : creatingIn,
    });

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    const story = result.data;
    upsert(story);
    setLayout((current) => ({ ...current, [story.columnId]: [...(current[story.columnId] ?? []), story.id] }));
    setCreatingIn(null);
    setSelectedId(story.id);
  }

  async function submitColumn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const name = String(data.get("name") ?? "");

    if (dialog?.kind === "create") {
      const result = await sendJson<Column>(`${base}/board/columns`, "POST", { name });

      if (!result.ok || !result.data) {
        setError(errorMessage(t, result.code, result.reason));
        return;
      }

      const created = result.data;
      setColumns((current) => [...current, created]);
      setLayout((current) => ({ ...current, [created.id]: [] }));
    }

    if (dialog?.kind === "rename") {
      const result = await sendJson<Column>(`${base}/board/columns/${dialog.column.id}`, "PUT", { name });

      if (!result.ok || !result.data) {
        setError(errorMessage(t, result.code, result.reason));
        return;
      }

      const renamed = result.data;
      setColumns((current) => current.map((column) => (column.id === renamed.id ? renamed : column)));
    }

    setDialog(null);
  }

  async function deleteColumn(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (dialog?.kind !== "delete") return;

    const data = new FormData(event.currentTarget);
    const moveTo = String(data.get("moveTo") ?? "");
    const hasStories = (layout[dialog.column.id] ?? []).length > 0;
    const result = await sendJson(
      `${base}/board/columns/${dialog.column.id}${hasStories ? `?moveTo=${encodeURIComponent(moveTo)}` : ""}`,
      "DELETE",
    );

    setDialog(null);

    if (!result.ok) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    await reload();
  }

  async function shiftColumn(column: Column, direction: -1 | 1) {
    const ids = columns.map((candidate) => candidate.id);
    const index = ids.indexOf(column.id);
    const target = index + direction;

    if (target < 0 || target >= ids.length) return;

    const next = arrayMove(columns, index, target);
    setColumns(next);

    const result = await sendJson(`${base}/board/columns/order`, "PUT", { columnIds: next.map((item) => item.id) });

    if (!result.ok) {
      setError(errorMessage(t, result.code, result.reason));
      await reload();
    }
  }

  async function syncPullRequests() {
    setSyncing(true);
    const result = await sendJson<Repository[]>(`${base}/repositories/sync`, "POST");
    setSyncing(false);

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    setSyncProblems(
      result.data
        .filter((repository) => repository.lastSyncError)
        .map((repository) =>
          t("board.repositoryError", {
            repository: repository.fullName,
            reason: githubReason(t, repository.lastSyncError ?? "UNEXPECTED"),
          }),
        ),
    );
    toast({ tone: "success", title: t("board.synced") });
    await reload();
  }

  async function moveToColumn(story: Story, columnId: string) {
    const result = await sendJson<Story>(`${base}/stories/${story.key}/move`, "PUT", { columnId });

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return false;
    }

    upsert(result.data);
    setLayout((current) => {
      const next = Object.fromEntries(Object.entries(current).map(([id, ids]) => [id, ids.filter((candidate) => candidate !== story.id)]));
      next[columnId] = [...(next[columnId] ?? []), story.id];
      return next;
    });
    return true;
  }

  const orderedColumns = [...columns].sort((a, b) => a.position - b.position);
  const deleting = dialog?.kind === "delete" ? dialog.column : null;
  const deletingHasStories = deleting ? (layout[deleting.id] ?? []).length > 0 : false;

  return (
    <div className={styles.wrapper}>
      <div className={styles.toolbar}>
        <StoryFilterBar
          filters={filters}
          onChange={setFilters}
          labels={labels}
          members={members}
          workspaceId={workspaceId}
          onPick={openPicked}
        />
        <div className={styles.toolbarActions}>
          {repositories.length > 0 ? (
            <Button size="sm" loading={syncing} icon={<RefreshCw size={14} aria-hidden="true" />} onClick={syncPullRequests}>
              {syncing ? t("board.syncing") : t("board.syncNow")}
            </Button>
          ) : null}
          {canManageBoard ? (
            <Button size="sm" icon={<Columns3 size={14} aria-hidden="true" />} onClick={() => setDialog({ kind: "create" })}>
              {t("board.addColumn")}
            </Button>
          ) : null}
          <Button appearance="primary" size="sm" icon={<Plus size={14} aria-hidden="true" />} onClick={() => setCreatingIn("first")}>
            {t("board.createStory")}
          </Button>
        </div>
      </div>

      {syncProblems.map((problem) => (
        <Alert key={problem}>{problem}</Alert>
      ))}
      {filtering ? <p className={settings.hint}>{t("board.filtersActive")}</p> : null}

      <DndContext
        id={`board-${workspaceId}`}
        sensors={sensors}
        collisionDetection={closestCorners}
        onDragStart={onDragStart}
        onDragOver={onDragOver}
        onDragEnd={onDragEnd}
        onDragCancel={() => {
          setActiveId(null);
          void reload();
        }}
      >
        <div className={styles.columns}>
          {orderedColumns.map((column, index) => {
            const ids = layout[column.id] ?? [];
            const visible = ids.map((id) => stories[id]).filter((story): story is Story => Boolean(story));

            return (
              <BoardColumn
              members={members}
                key={column.id}
                column={column}
                stories={visible.filter((story) => matchesStoryFilters(story, filters))}
                allCount={ids.length}
                filtering={filtering}
                landedId={landedId}
                dragDisabled={filtering}
                canManage={canManageBoard}
                isFirst={index === 0}
                isLast={index === orderedColumns.length - 1}
                onOpen={(story) => setSelectedId(story.id)}
                onCreate={setCreatingIn}
                onRename={(target) => setDialog({ kind: "rename", column: target })}
                onDelete={(target) => setDialog({ kind: "delete", column: target })}
                onShift={shiftColumn}
              />
            );
          })}
        </div>
        <DragOverlay>
          {active ? (
            <div className={`${styles.card} ${styles.overlay}`}>
              <CardContent story={active} assignee={members.find((member) => member.id === active.assigneeId)} status={columns.find((column) => column.id === active.columnId)?.name} t={t} />
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      <Modal open={creatingIn !== null} title={t("board.createStory")} onClose={() => setCreatingIn(null)}>
        <form className={modal.form} onSubmit={createStory}>
          <TextField
            label={t("board.storyTitle")}
            name="title"
            maxLength={200}
            placeholder={t("board.storyTitlePlaceholder")}
            required
            autoFocus
          />
          <label className={modal.field}>
            <span>{t("story.priority")}</span>
            <Select name="priority" defaultValue="MEDIUM">
              {PRIORITIES.map((priority) => (
                <option key={priority} value={priority}>
                  {t(`priority.${priority}`)}
                </option>
              ))}
            </Select>
          </label>
          <div className={modal.actions}>
            <Button type="button" appearance="subtle" onClick={() => setCreatingIn(null)}>
              {t("board.cancel")}
            </Button>
            <Button type="submit" appearance="primary">
              {t("board.create")}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={dialog?.kind === "create" || dialog?.kind === "rename"}
        title={dialog?.kind === "rename" ? t("board.rename") : t("board.addColumn")}
        onClose={() => setDialog(null)}
      >
        <form className={modal.form} onSubmit={submitColumn}>
          <TextField
            label={t("board.columnName")}
            name="name"
            maxLength={60}
            placeholder={t("board.columnNamePlaceholder")}
            defaultValue={dialog?.kind === "rename" ? dialog.column.name : ""}
            required
            autoFocus
          />
          <div className={modal.actions}>
            <Button type="button" appearance="subtle" onClick={() => setDialog(null)}>
              {t("board.cancel")}
            </Button>
            <Button type="submit" appearance="primary">
              {t("board.save")}
            </Button>
          </div>
        </form>
      </Modal>

      <Modal
        open={deleting !== null}
        title={t("board.deleteColumnTitle", { name: deleting?.name ?? "" })}
        onClose={() => setDialog(null)}
      >
        <form className={modal.form} onSubmit={deleteColumn}>
          <p className={modal.lead}>{deletingHasStories ? t("board.deleteColumnMove") : t("board.deleteColumnEmpty")}</p>
          {deletingHasStories ? (
            <label className={modal.field}>
              <span>{t("board.moveTo")}</span>
              <Select name="moveTo" required>
                {orderedColumns
                  .filter((column) => column.id !== deleting?.id)
                  .map((column) => (
                    <option key={column.id} value={column.id}>
                      {column.name}
                    </option>
                  ))}
              </Select>
            </label>
          ) : null}
          <div className={modal.actions}>
            <Button type="button" appearance="subtle" onClick={() => setDialog(null)}>
              {t("board.cancel")}
            </Button>
            <Button type="submit" appearance="danger">
              {t("board.deleteColumn")}
            </Button>
          </div>
        </form>
      </Modal>

      {selected ? (
        <StoryDrawer
          key={selected.id}
          members={members}
          workspaceId={workspaceId}
          story={selected}
          columns={columns}
          repositories={repositories}
          hasGithubToken={hasGithubToken}
          onChange={upsert}
          onMoveToColumn={moveToColumn}
          onRepositoriesChanged={reload}
          onDeleted={() => {
            setLayout((current) => ({
              ...current,
              [selected.columnId]: (current[selected.columnId] ?? []).filter((id) => id !== selected.id),
            }));
            closeStory();
          }}
          onClose={closeStory}
        />
      ) : null}
    </div>
  );
}
