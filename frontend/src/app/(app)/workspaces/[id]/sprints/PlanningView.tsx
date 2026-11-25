"use client";

import { DndContext, DragOverlay, PointerSensor, useSensor, useSensors, type DragEndEvent, type DragStartEvent } from "@dnd-kit/core";
import { BarChart3, MoreHorizontal, Pencil, Plus, Search, Trash2 } from "lucide-react";
import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { DropdownContent, DropdownItem, DropdownMenu, DropdownTrigger } from "@/components/ui/DropdownMenu";
import { IconButton } from "@/components/ui/IconButton";
import { Modal } from "@/components/ui/Modal";
import { useToast } from "@/components/ui/Toast";
import { Tooltip } from "@/components/ui/Tooltip";
import { errorMessage, useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import type { Board, Member, Repository, Sprint, Story } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { normalizeText, toggle } from "@/lib/story-filters";
import { replaceSearchParams } from "@/lib/url-state";
import { formatDate } from "@/lib/workspace-types";
import { StoryDrawer } from "../board/StoryDrawer";
import { ClosedSprints } from "./ClosedSprints";
import { VelocityChart } from "./VelocityChart";
import { IssueContent } from "./IssueRow";
import { PlanSection } from "./PlanSection";
import { BACKLOG, groupOf } from "./planning";
import { SprintConfirm } from "./SprintConfirm";
import { addDays, SprintFormModal } from "./SprintFormModal";
import { SprintPanel } from "./SprintPanel";
import { localToday, sprintDays } from "./sprint-metrics";
import styles from "./planning.module.css";

type PlanningViewProps = {
  workspaceId: string;
  board: Board;
  members: Member[];
  repositories: Repository[];
  hasGithubToken: boolean;
  initialSprints: Sprint[];
  canManage: boolean;
};

type Confirm = { kind: "close" | "delete"; sprint: Sprint };

const BACKLOG_STEP = 50;

function byStart(a: Sprint, b: Sprint) {
  if (a.state !== b.state) return a.state === "ACTIVE" ? -1 : 1;
  return a.startDate.localeCompare(b.startDate);
}

export function PlanningView({ workspaceId, board, members, repositories: initialRepositories, hasGithubToken, initialSprints, canManage }: PlanningViewProps) {
  const { t, locale } = useI18n();
  const toast = useToast();
  const params = useSearchParams();
  const base = `/api/workspaces/${workspaceId}`;
  const [sprints, setSprints] = useState(() => [...initialSprints].sort(byStart));
  const [stories, setStories] = useState(board.stories);
  const [repositories, setRepositories] = useState(initialRepositories);
  const appliedText = params.get("q") ?? "";
  const [text, setText] = useState(appliedText);
  const [syncedText, setSyncedText] = useState(appliedText);
  const [people, setPeople] = useState<string[]>([]);
  const [backlogLimit, setBacklogLimit] = useState(BACKLOG_STEP);
  const [dragging, setDragging] = useState<Story | null>(null);
  const [report, setReport] = useState<Sprint | null>(null);
  const [editing, setEditing] = useState<Sprint | "new" | null>(null);
  const [confirm, setConfirm] = useState<Confirm | null>(null);
  const [busy, setBusy] = useState(false);
  const [starting, setStarting] = useState<string | null>(null);
  const [error, setError] = useState<string>();
  const [closedVersion, setClosedVersion] = useState(0);

  if (appliedText !== syncedText) {
    setSyncedText(appliedText);
    setText(appliedText);
  }

  const storyKey = params.get("story");
  const reportId = params.get("sprint");
  const opened = storyKey ? stories.find((story) => story.key === storyKey) ?? null : null;

  const columns = useMemo(() => new Map(board.columns.map((column) => [column.id, column])), [board.columns]);
  const memberMap = useMemo(() => new Map(members.map((member) => [member.id, member])), [members]);
  const sensors = useSensors(useSensor(PointerSensor, { activationConstraint: { distance: 6 } }));
  const active = sprints.find((sprint) => sprint.state === "ACTIVE");
  const date = (value: string) => formatDate(value, dateLocale(locale));

  useEffect(() => {
    if (!reportId) {
      setReport(null);
      return;
    }

    const known = sprints.find((sprint) => sprint.id === reportId);
    if (known) {
      setReport(known);
      return;
    }

    let current = true;
    sendJson<Sprint>(`${base}/sprints/${encodeURIComponent(reportId)}`, "GET").then((response) => {
      if (!current) return;
      if (response.ok && response.data) setReport(response.data);
      else replaceSearchParams((search) => search.delete("sprint"));
    });

    return () => {
      current = false;
    };
  }, [base, reportId, sprints]);

  const visible = useMemo(() => {
    const term = normalizeText(text.trim());

    return stories
      .filter((story) => people.length === 0 || (story.assigneeId !== null && people.includes(story.assigneeId)))
      .filter((story) => term === "" || normalizeText(story.title).includes(term) || story.key.toLowerCase().includes(term))
      .sort((a, b) => a.createdAt.localeCompare(b.createdAt));
  }, [stories, text, people]);

  const inSprint = (sprintId: string) => visible.filter((story) => story.sprintId === sprintId);
  const backlog = visible.filter((story) => story.sprintId === null && groupOf(columns.get(story.columnId)?.kind) !== "done");
  const backlogTotal = stories.filter((story) => story.sprintId === null && groupOf(columns.get(story.columnId)?.kind) !== "done").length;

  const upsert = useCallback((story: Story) => {
    setStories((current) => current.map((candidate) => (candidate.id === story.id ? { ...candidate, ...story } : candidate)));
  }, []);

  async function move(story: Story, sprintId: string | null) {
    if (story.sprintId === sprintId) return;

    upsert({ ...story, sprintId });
    const response = await sendJson<Story>(`${base}/stories/${story.key}/sprint`, "PUT", { sprintId });

    if (!response.ok || !response.data) {
      upsert(story);
      toast({ tone: "danger", title: errorMessage(t, response.code, response.reason) });
      return;
    }

    upsert(response.data);
  }

  function onDragStart(event: DragStartEvent) {
    setDragging(stories.find((story) => story.id === event.active.id) ?? null);
  }

  function onDragEnd(event: DragEndEvent) {
    setDragging(null);
    const story = stories.find((candidate) => candidate.id === event.active.id);
    const target = event.over?.id;

    if (!story || target === undefined) return;

    void move(story, target === BACKLOG ? null : String(target));
  }

  function openStory(story: Story) {
    replaceSearchParams((search) => search.set("story", story.key));
  }

  function closeStory() {
    replaceSearchParams((search) => search.delete("story"));
  }

  function openReport(sprint: Sprint) {
    setReport(sprint);
    replaceSearchParams((search) => search.set("sprint", sprint.id));
  }

  function closeReport() {
    setReport(null);
    setError(undefined);
    replaceSearchParams((search) => search.delete("sprint"));
  }

  function replaceSprint(updated: Sprint) {
    setSprints((current) =>
      (current.some((sprint) => sprint.id === updated.id)
        ? current.map((sprint) => (sprint.id === updated.id ? updated : sprint))
        : [...current, updated]
      )
        .filter((sprint) => sprint.state !== "CLOSED")
        .sort(byStart),
    );
    setReport((current) => (current?.id === updated.id ? updated : current));
  }

  async function run(sprint: Sprint, action: "start" | "close") {
    setBusy(true);
    setStarting(action === "start" ? sprint.id : null);
    setError(undefined);
    const response = await sendJson<Sprint>(`${base}/sprints/${sprint.id}/${action}`, "POST");
    setBusy(false);
    setStarting(null);
    setConfirm(null);

    if (!response.ok || !response.data) {
      const message = response.code === "conflict" ? t("sprints.activeConflict") : errorMessage(t, response.code);
      if (report) setError(message);
      else toast({ tone: "danger", title: message });
      return;
    }

    if (action === "close") {
      setStories((current) =>
        current.map((story) =>
          story.sprintId === sprint.id && groupOf(columns.get(story.columnId)?.kind) !== "done" ? { ...story, sprintId: null } : story,
        ),
      );
      setClosedVersion((value) => value + 1);
    }

    replaceSprint(response.data);
    toast({ tone: "success", title: t(action === "start" ? "sprints.started" : "sprints.closedToast", { name: sprint.name }) });
  }

  async function remove(sprint: Sprint) {
    setBusy(true);
    const response = await sendJson(`${base}/sprints/${sprint.id}`, "DELETE");
    setBusy(false);
    setConfirm(null);

    if (!response.ok) {
      toast({ tone: "danger", title: errorMessage(t, response.code) });
      return;
    }

    setSprints((current) => current.filter((candidate) => candidate.id !== sprint.id));
    setStories((current) => current.map((story) => (story.sprintId === sprint.id ? { ...story, sprintId: null } : story)));
    if (report?.id === sprint.id) closeReport();
    toast({ tone: "success", title: t("sprints.deleted") });
  }

  async function moveToColumn(story: Story, columnId: string) {
    const response = await sendJson<Story>(`${base}/stories/${story.key}/move`, "PUT", { columnId });

    if (!response.ok || !response.data) {
      toast({ tone: "danger", title: errorMessage(t, response.code, response.reason) });
      return false;
    }

    upsert(response.data);
    return true;
  }

  const reloadRepositories = useCallback(async () => {
    const response = await sendJson<Repository[]>(`${base}/repositories`, "GET");
    if (response.ok && response.data) setRepositories(response.data);
  }, [base]);

  const suggestion = useMemo(() => {
    const today = localToday();
    const latest = sprints.reduce<Sprint | null>((last, sprint) => (!last || sprint.endDate > last.endDate ? sprint : last), null);
    const next = latest ? addDays(latest.endDate, 1) : today;
    return { name: `Sprint ${sprints.length + 1}`, start: next > today ? next : today };
  }, [sprints]);

  function sprintActions(sprint: Sprint) {
    const days = sprintDays(sprint, localToday());

    return (
      <>
        {sprint.state === "ACTIVE" && canManage ? (
          <Button size="sm" onClick={() => setConfirm({ kind: "close", sprint })}>
            {t("sprints.complete")}
          </Button>
        ) : null}
        {sprint.state === "PLANNED" && canManage ? (
          active ? (
            <Tooltip content={t("sprints.activeConflict")}>
              <span>
                <Button size="sm" disabled>
                  {t("sprints.start")}
                </Button>
              </span>
            </Tooltip>
          ) : (
            <Button size="sm" appearance="primary" loading={starting === sprint.id} onClick={() => void run(sprint, "start")}>
              {t("sprints.start")}
            </Button>
          )
        ) : null}
        {sprint.state === "ACTIVE" ? <span className={styles.daysLeft}>{t("sprints.daysLeft", { count: days.remaining })}</span> : null}
        <DropdownMenu>
          <DropdownTrigger>
            <IconButton size="sm" label={t("story.moreActions")} icon={<MoreHorizontal size={16} aria-hidden="true" />} />
          </DropdownTrigger>
          <DropdownContent>
            <DropdownItem icon={<BarChart3 size={16} aria-hidden="true" />} onSelect={() => openReport(sprint)}>
              {t("sprints.report")}
            </DropdownItem>
            {canManage ? (
              <DropdownItem icon={<Pencil size={16} aria-hidden="true" />} onSelect={() => setEditing(sprint)}>
                {t("sprints.edit")}
              </DropdownItem>
            ) : null}
            {canManage && sprint.state === "PLANNED" ? (
              <DropdownItem icon={<Trash2 size={16} aria-hidden="true" />} danger onSelect={() => setConfirm({ kind: "delete", sprint })}>
                {t("sprints.delete")}
              </DropdownItem>
            ) : null}
          </DropdownContent>
        </DropdownMenu>
      </>
    );
  }

  return (
    <div className={styles.view}>
      <div className={styles.toolbar}>
        <span className={styles.searchWrap}>
          <Search size={14} aria-hidden="true" className={styles.searchIcon} />
          <input
            className={styles.search}
            type="search"
            value={text}
            placeholder={t("sprints.searchIssues")}
            aria-label={t("sprints.searchIssues")}
            onChange={(event) => setText(event.target.value)}
          />
        </span>
        <span className={styles.people} role="group" aria-label={t("filters.assignee")}>
          {members.map((member) => (
            <Tooltip key={member.id} content={member.name}>
              <button
                type="button"
                className={styles.person}
                aria-pressed={people.includes(member.id)}
                aria-label={member.name}
                onClick={() => setPeople((current) => toggle(current, member.id))}
              >
                <Avatar name={member.name} seed={member.id} src={member.avatarUrl} size="sm" />
              </button>
            </Tooltip>
          ))}
        </span>
        {text || people.length > 0 ? (
          <Button size="sm" appearance="subtle" onClick={() => { setText(""); setPeople([]); }}>
            {t("board.clearFilters")}
          </Button>
        ) : null}
      </div>

      <DndContext id={`planning-${workspaceId}`} sensors={sensors} onDragStart={onDragStart} onDragEnd={onDragEnd} onDragCancel={() => setDragging(null)}>
        <div className={styles.sections}>
          {sprints.map((sprint) => (
            <PlanSection
              key={sprint.id}
              id={sprint.id}
              active={sprint.state === "ACTIVE"}
              title={
                <button type="button" className={styles.nameLink} onClick={() => openReport(sprint)}>
                  {sprint.name}
                </button>
              }
              meta={t("sprints.dates", { start: date(sprint.startDate), end: date(sprint.endDate) })}
              goal={sprint.goal}
              actions={sprintActions(sprint)}
              stories={inSprint(sprint.id)}
              total={stories.filter((story) => story.sprintId === sprint.id).length}
              columns={columns}
              members={memberMap}
              targets={sprints}
              inBacklog={false}
              empty={t("sprints.emptySprint")}
              onOpen={openStory}
              onMove={(story, target) => void move(story, target)}
            />
          ))}

          <PlanSection
            id={BACKLOG}
            title={t("sprints.backlog")}
            actions={
              canManage ? (
                <Button size="sm" icon={<Plus size={14} aria-hidden="true" />} onClick={() => setEditing("new")}>
                  {t("sprints.create")}
                </Button>
              ) : null
            }
            stories={backlog}
            total={backlogTotal}
            columns={columns}
            members={memberMap}
            targets={sprints}
            inBacklog
            empty={t("sprints.emptyBacklog")}
            limit={backlogLimit}
            onShowMore={() => setBacklogLimit((value) => value + BACKLOG_STEP)}
            onOpen={openStory}
            onMove={(story, target) => void move(story, target)}
          />
        </div>

        <DragOverlay dropAnimation={null}>
          {dragging ? (
            <div className={`${styles.row} ${styles.overlay}`}>
              <IssueContent
                story={dragging}
                column={columns.get(dragging.columnId)}
                assignee={dragging.assigneeId ? memberMap.get(dragging.assigneeId) : undefined}
              />
            </div>
          ) : null}
        </DragOverlay>
      </DndContext>

      <VelocityChart
        workspaceId={workspaceId}
        version={closedVersion}
        onOpen={(sprintId) => replaceSearchParams((search) => search.set("sprint", sprintId))}
      />

      <ClosedSprints workspaceId={workspaceId} version={closedVersion} onOpen={openReport} />

      {opened ? (
        <StoryDrawer
          key={opened.id}
          workspaceId={workspaceId}
          story={opened}
          columns={board.columns}
          members={members}
          sprints={sprints}
          repositories={repositories}
          hasGithubToken={hasGithubToken}
          onChange={upsert}
          onMoveToColumn={moveToColumn}
          onRepositoriesChanged={reloadRepositories}
          onDeleted={() => {
            setStories((current) => current.filter((story) => story.id !== opened.id));
            closeStory();
          }}
          onClose={closeStory}
        />
      ) : null}

      {report ? (
        <Modal open size="wide" hideTitle title={report.name} onClose={closeReport}>
          {error ? <Alert>{error}</Alert> : null}
          <SprintPanel
            workspaceId={workspaceId}
            sprint={report}
            stories={stories}
            columns={board.columns}
            members={members}
            canManage={canManage}
            canStart={!active}
            starting={starting === report.id}
            onStart={() => void run(report, "start")}
            onClose={() => setConfirm({ kind: "close", sprint: report })}
            onEdit={() => setEditing(report)}
            onDelete={() => setConfirm({ kind: "delete", sprint: report })}
            onDismiss={closeReport}
          />
        </Modal>
      ) : null}

      {editing ? (
        <SprintFormModal
          workspaceId={workspaceId}
          sprint={editing === "new" ? null : editing}
          suggestedName={suggestion.name}
          suggestedStart={suggestion.start}
          onSaved={(saved) => {
            setEditing(null);
            replaceSprint(saved);
          }}
          onClose={() => setEditing(null)}
        />
      ) : null}

      {confirm ? (
        <SprintConfirm
          title={t(confirm.kind === "close" ? "sprints.closeTitle" : "sprints.deleteTitle", { name: confirm.sprint.name })}
          description={confirm.kind === "close" ? t("sprints.closeText") : t("sprints.deleteText")}
          confirmLabel={confirm.kind === "close" ? t("sprints.complete") : t("sprints.delete")}
          tone={confirm.kind === "close" ? "primary" : "danger"}
          busy={busy}
          onConfirm={() => void (confirm.kind === "close" ? run(confirm.sprint, "close") : remove(confirm.sprint))}
          onCancel={() => setConfirm(null)}
        />
      ) : null}
    </div>
  );
}
