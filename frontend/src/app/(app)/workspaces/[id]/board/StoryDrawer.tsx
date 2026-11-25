"use client";

import { Copy, ExternalLink, GitMerge, GitPullRequest, MoreHorizontal, Plus, Trash2, X } from "lucide-react";
import Link from "next/link";
import { useMemo, useState, type FormEvent, type KeyboardEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Drawer } from "@/components/ui/Drawer";
import { DropdownContent, DropdownItem, DropdownMenu, DropdownTrigger } from "@/components/ui/DropdownMenu";
import { IconButton } from "@/components/ui/IconButton";
import { Markdown } from "@/components/ui/Markdown";
import { Select } from "@/components/ui/Select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { TextArea } from "@/components/ui/TextArea";
import { TextField } from "@/components/ui/TextField";
import { useToast } from "@/components/ui/Toast";
import { errorMessage, useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import {
  PRIORITIES,
  type ChecklistItem,
  type Column,
  type Member,
  type Priority,
  type PullRequestState,
  type Repository,
  type Sprint,
  type Story,
} from "@/lib/board-types";
import { isHttpUrl, parseLabels } from "@/lib/board-utils";
import { sendJson } from "@/lib/client";
import { formatDate } from "@/lib/workspace-types";
import { ActivityFeed } from "./ActivityFeed";
import { Comments } from "./Comments";
import { Subtasks } from "./Subtasks";
import styles from "./StoryDrawer.module.css";

type StoryDrawerProps = {
  workspaceId: string;
  story: Story;
  columns: Column[];
  members: Member[];
  sprints: Sprint[];
  repositories: Repository[];
  hasGithubToken: boolean;
  onChange: (story: Story) => void;
  onMoveToColumn: (story: Story, columnId: string) => Promise<boolean>;
  onRepositoriesChanged: () => Promise<void>;
  onDeleted: () => void;
  onClose: () => void;
};

const PR_TONE: Record<PullRequestState, BadgeTone> = { MERGED: "success", OPEN: "info", CLOSED: "neutral" };
const PR_ICON = { MERGED: GitMerge, OPEN: GitPullRequest, CLOSED: GitPullRequest } as const;

export function StoryDrawer({
  workspaceId,
  story,
  columns,
  members,
  sprints,
  repositories,
  hasGithubToken,
  onChange,
  onMoveToColumn,
  onRepositoriesChanged,
  onDeleted,
  onClose,
}: StoryDrawerProps) {
  const { t, locale } = useI18n();
  const toast = useToast();
  const base = `/api/workspaces/${workspaceId}/stories/${story.key}`;
  const [title, setTitle] = useState(story.title);
  const [description, setDescription] = useState(story.description ?? "");
  const [priority, setPriority] = useState<Priority>(story.priority);
  const [points, setPoints] = useState(story.storyPoints === null ? "" : String(story.storyPoints));
  const [labels, setLabels] = useState(story.labels.join(", "));
  const [criteria, setCriteria] = useState<ChecklistItem[]>(story.acceptanceCriteria);
  const [fields, setFields] = useState<Record<string, string>>({});
  const [error, setError] = useState<string>();
  const [saving, setSaving] = useState(false);
  const [confirm, setConfirm] = useState<"delete" | "discard" | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [githubError, setGithubError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [subtaskChanges, setSubtaskChanges] = useState(0);

  const repository = repositories.find((candidate) => candidate.id === story.repositoryId);
  const plannable = useMemo(
    () => sprints.filter((sprint) => sprint.state !== "CLOSED" || sprint.id === story.sprintId),
    [sprints, story.sprintId],
  );
  const orderedColumns = useMemo(() => [...columns].sort((a, b) => a.position - b.position), [columns]);

  const dirty =
    title !== story.title ||
    description !== (story.description ?? "") ||
    priority !== story.priority ||
    points !== (story.storyPoints === null ? "" : String(story.storyPoints)) ||
    JSON.stringify(parseLabels(labels)) !== JSON.stringify(story.labels) ||
    JSON.stringify(criteria) !== JSON.stringify(story.acceptanceCriteria);

  function requestClose() {
    if (dirty) {
      setConfirm("discard");
      return;
    }

    onClose();
  }

  async function save(event?: FormEvent<HTMLFormElement>) {
    event?.preventDefault();
    setSaving(true);
    setError(undefined);
    setFields({});

    const result = await sendJson<Story>(base, "PUT", {
      title,
      description,
      priority,
      storyPoints: points === "" ? null : Number(points),
      labels: parseLabels(labels),
      acceptanceCriteria: criteria,
    });

    setSaving(false);

    if (result.ok && result.data) {
      onChange({ ...result.data, subtasks: result.data.subtasks ?? story.subtasks });
      setTitle(result.data.title);
      setCriteria(result.data.acceptanceCriteria);
      toast({ tone: "success", title: t("story.saved") });
      return;
    }

    setFields(result.fields);
    setError(Object.keys(result.fields).length === 0 ? errorMessage(t, result.code, result.reason) : undefined);
  }

  async function remove() {
    setDeleting(true);
    const result = await sendJson(base, "DELETE");
    setDeleting(false);

    if (!result.ok) {
      setConfirm(null);
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    onDeleted();
  }

  async function changeStatus(columnId: string) {
    const column = orderedColumns.find((candidate) => candidate.id === columnId);

    if (await onMoveToColumn(story, columnId)) {
      toast({ tone: "success", title: t("story.moved", { column: column?.name ?? "" }) });
    }
  }

  async function assign(assigneeId: string) {
    const result = await sendJson<Story>(`${base}/assignee`, "PUT", { assigneeId: assigneeId === "" ? null : assigneeId });

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    onChange(result.data);
  }

  async function plan(sprintId: string) {
    const result = await sendJson<Story>(`${base}/sprint`, "PUT", { sprintId: sprintId === "" ? null : sprintId });

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    onChange(result.data);
  }

  async function copyKey() {
    try {
      await navigator.clipboard.writeText(story.key);
      toast({ tone: "success", title: t("story.keyCopied") });
    } catch {
      /* clipboard unavailable: nothing to do */
    }
  }

  async function github<T>(path: string, method: string, body: unknown, after?: (data: T) => void) {
    setBusy(true);
    setGithubError(undefined);
    const result = await sendJson<T>(`${base}/${path}`, method, body);
    setBusy(false);

    if (!result.ok || !result.data) {
      setGithubError(errorMessage(t, result.code, result.reason));
      return;
    }

    after?.(result.data);
  }

  function linkRepository(repositoryId: string) {
    return github<Story>("repository", "PUT", { repositoryId: repositoryId === "" ? null : repositoryId }, onChange);
  }

  async function createRepository(name: string, isPrivate: boolean) {
    await github<Story>("new-repository", "POST", { name, isPrivate }, (updated) => {
      onChange(updated);
      void onRepositoriesChanged();
    });
  }

  function onFormKeyDown(event: KeyboardEvent<HTMLFormElement>) {
    if (event.key === "Enter" && (event.metaKey || event.ctrlKey) && dirty) {
      event.preventDefault();
      void save();
    }
  }

  const stamp = (iso: string) => formatDate(iso, dateLocale(locale));

  return (
    <>
      <Drawer
        open
        width="dialog"
        label={t("story.panelLabel")}
        onClose={requestClose}
        title={<span className={styles.key}>{story.key}</span>}
        headerActions={
          <DropdownMenu>
            <DropdownTrigger>
              <IconButton size="sm" label={t("story.moreActions")} icon={<MoreHorizontal size={18} aria-hidden="true" />} />
            </DropdownTrigger>
            <DropdownContent>
              <DropdownItem icon={<Copy size={16} aria-hidden="true" />} onSelect={copyKey}>
                {t("story.copyKey")}
              </DropdownItem>
              <DropdownItem icon={<Trash2 size={16} aria-hidden="true" />} danger onSelect={() => setConfirm("delete")}>
                {t("story.delete")}
              </DropdownItem>
            </DropdownContent>
          </DropdownMenu>
        }
      >
        <form className={styles.layout} onSubmit={save} onKeyDown={onFormKeyDown}>
          <div className={styles.main}>
            {error ? <Alert>{error}</Alert> : null}

            <div className={styles.titleField}>
              <TextField
                label={t("story.title")}
                hideLabel
                value={title}
                maxLength={200}
                error={fields.title}
                onChange={(event) => setTitle(event.target.value)}
                required
              />
            </div>

            <div className={styles.statusBar} role="group" aria-label={t("story.status")}>
              {orderedColumns.map((column) => (
                <button
                  key={column.id}
                  type="button"
                  className={`${styles.statusButton} ${column.id === story.columnId ? styles.statusActive : ""}`}
                  aria-pressed={column.id === story.columnId}
                  onClick={() => (column.id === story.columnId ? undefined : void changeStatus(column.id))}
                >
                  {column.name}
                </button>
              ))}
            </div>

            <section className={styles.details}>
              <label className={styles.field}>
                <span>{t("story.assignee")}</span>
                <Select value={story.assigneeId ?? ""} onChange={(event) => void assign(event.target.value)}>
                  <option value="">{t("story.unassigned")}</option>
                  {members.map((member) => (
                    <option key={member.id} value={member.id}>
                      {member.name}
                    </option>
                  ))}
                </Select>
              </label>
              <label className={styles.field}>
                <span>{t("sprints.sprint")}</span>
                <Select value={story.sprintId ?? ""} onChange={(event) => void plan(event.target.value)}>
                  <option value="">{t("sprints.backlog")}</option>
                  {plannable.map((sprint) => (
                    <option key={sprint.id} value={sprint.id} disabled={sprint.state === "CLOSED"}>
                      {sprint.state === "ACTIVE" ? `${sprint.name} · ${t("sprints.states.ACTIVE")}` : sprint.name}
                    </option>
                  ))}
                </Select>
              </label>
              <label className={styles.field}>
                <span>{t("story.priority")}</span>
                <Select value={priority} onChange={(event) => setPriority(event.target.value as Priority)}>
                  {PRIORITIES.map((value) => (
                    <option key={value} value={value}>
                      {t(`priority.${value}`)}
                    </option>
                  ))}
                </Select>
              </label>
              <label className={styles.field}>
                <span>{t("story.points")}</span>
                <input
                  className={styles.input}
                  type="number"
                  min={0}
                  max={1000}
                  value={points}
                  aria-invalid={fields.storyPoints ? true : undefined}
                  onChange={(event) => setPoints(event.target.value)}
                />
                {fields.storyPoints ? <small className={styles.fieldError} role="alert">{fields.storyPoints}</small> : null}
              </label>
              <label className={styles.field}>
                <span>{t("story.labels")}</span>
                <input
                  className={styles.input}
                  value={labels}
                  placeholder={t("story.labelsHint")}
                  aria-invalid={fields.labels ? true : undefined}
                  onChange={(event) => setLabels(event.target.value)}
                />
                {fields.labels ? <small className={styles.fieldError} role="alert">{fields.labels}</small> : null}
              </label>
            </section>


            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>{t("story.description")}</h3>
              <Tabs defaultValue="write">
                <TabsList aria-label={t("story.description")}>
                  <TabsTrigger value="write">{t("story.write")}</TabsTrigger>
                  <TabsTrigger value="preview">{t("story.preview")}</TabsTrigger>
                </TabsList>
                <TabsContent value="write">
                  <TextArea
                    label={t("story.description")}
                    hideLabel
                    value={description}
                    maxLength={20000}
                    rows={8}
                    placeholder={t("story.descriptionPlaceholder")}
                    error={fields.description}
                    onChange={(event) => setDescription(event.target.value)}
                  />
                </TabsContent>
                <TabsContent value="preview">
                  {description.trim() ? <Markdown>{description}</Markdown> : <p className={styles.hint}>{t("story.nothingToPreview")}</p>}
                </TabsContent>
              </Tabs>
            </section>

            <Checklist title={t("story.acceptanceCriteria")} placeholder={t("story.criterionPlaceholder")} items={criteria} onChange={setCriteria} />
            <Subtasks
              workspaceId={workspaceId}
              story={story}
              repository={repository}
              onProgress={(progress) => {
                onChange({ ...story, subtasks: progress });
                setSubtaskChanges((count) => count + 1);
              }}
            />
            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>{t("story.pullRequests")}</h3>
              {story.pullRequests.length === 0 ? (
                <p className={styles.hint}>{t("story.noPullRequests", { key: story.key })}</p>
              ) : (
                <ul className={styles.prList}>
                  {story.pullRequests.map((pullRequest) => {
                    const Icon = PR_ICON[pullRequest.state];

                    return (
                      <li key={`${pullRequest.repositoryId}-${pullRequest.number}`} className={styles.pr}>
                        <Icon size={16} aria-hidden="true" className={styles.prIcon} />
                        <span className={styles.prText}>
                          {isHttpUrl(pullRequest.url) ? (
                            <a href={pullRequest.url} target="_blank" rel="noopener noreferrer">
                              #{pullRequest.number} {pullRequest.title}
                              <ExternalLink size={12} aria-hidden="true" />
                            </a>
                          ) : (
                            <span>
                              #{pullRequest.number} {pullRequest.title}
                            </span>
                          )}
                        </span>
                        <Badge tone={PR_TONE[pullRequest.state]}>{t(`story.prState.${pullRequest.state}`)}</Badge>
                      </li>
                    );
                  })}
                </ul>
              )}
            </section>
            <Comments workspaceId={workspaceId} scope={`stories/${story.key}`} />
            <ActivityFeed
              workspaceId={workspaceId}
              scope={`stories/${story.key}`}
              version={`${story.updatedAt}|${story.columnId}|${story.assigneeId}|${subtaskChanges}`}
            />
          </div>

          <aside className={styles.side}>
            <section className={styles.section} aria-label={t("story.github")}>
              <h3 className={styles.sectionTitle}>{t("story.github")}</h3>
              {githubError ? <Alert>{githubError}</Alert> : null}

              {repositories.length === 0 ? (
                <p className={styles.hint}>
                  {hasGithubToken ? t("story.needRepositories") : t("story.needToken")}{" "}
                  <Link href={hasGithubToken ? `/workspaces/${workspaceId}/repositories` : "/settings"}>
                    {hasGithubToken ? t("story.manageRepositories") : t("nav.settings")}
                  </Link>
                </p>
              ) : (
                <label className={styles.field}>
                  <span>{t("story.repository")}</span>
                  <Select value={story.repositoryId ?? ""} disabled={busy} onChange={(event) => void linkRepository(event.target.value)}>
                    <option value="">{t("story.noRepository")}</option>
                    {repositories.map((candidate) => (
                      <option key={candidate.id} value={candidate.id ?? ""}>
                        {candidate.fullName}
                      </option>
                    ))}
                  </Select>
                </label>
              )}

              {hasGithubToken && !story.repositoryId ? (
                <NewRepositoryForm defaultName={story.key.toLowerCase()} busy={busy} onCreate={createRepository} />
              ) : null}

            </section>

            <section className={styles.section}>
              <h3 className={styles.sectionTitle}>{t("story.agile")}</h3>
              <Link href={`/workspaces/${workspaceId}/board`}>{t("story.viewOnBoard")}</Link>
            </section>

            <p className={styles.meta}>
              {t("story.createdAt", { date: stamp(story.createdAt) })}
              <br />
              {t("story.updatedAt", { date: stamp(story.updatedAt) })}
            </p>
          </aside>

          <footer className={styles.footer}>
            {dirty ? <span className={styles.unsaved}>{t("story.unsaved")}</span> : null}
            <Button type="button" appearance="subtle" onClick={requestClose}>
              {t("story.close")}
            </Button>
            <Button type="submit" appearance="primary" loading={saving} disabled={!dirty}>
              {t("story.save")}
            </Button>
          </footer>
        </form>

        <ConfirmDialog
          open={confirm === "delete"}
          title={t("story.deleteTitle", { key: story.key })}
          description={t("story.deleteLead")}
          confirmLabel={t("story.delete")}
          busy={deleting}
          onConfirm={remove}
          onCancel={() => setConfirm(null)}
        />
        <ConfirmDialog
          open={confirm === "discard"}
          title={t("story.discardTitle")}
          description={t("story.discardLead")}
          confirmLabel={t("story.discard")}
          cancelLabel={t("story.keepEditing")}
          onConfirm={onClose}
          onCancel={() => setConfirm(null)}
        />
      </Drawer>
    </>
  );
}

function NewRepositoryForm({
  defaultName,
  busy,
  onCreate,
}: {
  defaultName: string;
  busy: boolean;
  onCreate: (name: string, isPrivate: boolean) => void;
}) {
  const { t } = useI18n();
  const [name, setName] = useState(defaultName);
  const [isPrivate, setIsPrivate] = useState(true);
  const valid = /^[A-Za-z0-9_.-]{1,100}$/.test(name);

  return (
    <details className={styles.newRepo}>
      <summary>{t("story.newRepository")}</summary>
      <div className={styles.stack}>
        <TextField label={t("story.repositoryName")} value={name} onChange={(event) => setName(event.target.value)} />
        <label className={styles.check}>
          <input type="checkbox" checked={isPrivate} onChange={(event) => setIsPrivate(event.target.checked)} />
          <span>{t("story.makePrivate")}</span>
        </label>
        <Button type="button" size="sm" loading={busy} disabled={!valid} onClick={() => onCreate(name, isPrivate)}>
          {t("story.createRepository")}
        </Button>
      </div>
    </details>
  );
}

function Checklist({
  title,
  placeholder,
  items,
  onChange,
}: {
  title: string;
  placeholder: string;
  items: ChecklistItem[];
  onChange: (items: ChecklistItem[]) => void;
}) {
  const { t } = useI18n();
  const [draft, setDraft] = useState("");
  const done = items.filter((item) => item.done).length;

  function add() {
    const text = draft.trim();
    if (text === "") return;
    onChange([...items, { id: null, text, done: false }]);
    setDraft("");
  }

  return (
    <section className={styles.section}>
      <h3 className={styles.sectionTitle}>
        {title}
        {items.length > 0 ? (
          <span className={styles.progressText}>
            {done}/{items.length}
          </span>
        ) : null}
      </h3>
      {items.length > 0 ? (
        <div className={styles.progress} role="progressbar" aria-valuemin={0} aria-valuemax={items.length} aria-valuenow={done} aria-label={title}>
          <span style={{ width: `${(done / items.length) * 100}%` }} />
        </div>
      ) : null}
      <ul className={styles.checkList}>
        {items.map((item, index) => (
          <li key={item.id ?? `new-${index}`} className={styles.checkItem}>
            <label className={styles.check}>
              <input
                type="checkbox"
                checked={item.done}
                onChange={(event) =>
                  onChange(items.map((candidate, position) => (position === index ? { ...candidate, done: event.target.checked } : candidate)))
                }
              />
              <span className={item.done ? styles.done : undefined}>{item.text}</span>
            </label>
            <IconButton
              size="sm"
              label={t("story.remove")}
              icon={<X size={14} aria-hidden="true" />}
              onClick={() => onChange(items.filter((_, position) => position !== index))}
            />
          </li>
        ))}
      </ul>
      <div className={styles.addRow}>
        <TextField
          label={title}
          hideLabel
          value={draft}
          placeholder={placeholder}
          maxLength={300}
          onChange={(event) => setDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === "Enter") {
              event.preventDefault();
              add();
            }
          }}
        />
        <Button type="button" size="sm" icon={<Plus size={14} aria-hidden="true" />} onClick={add}>
          {t("story.addItem")}
        </Button>
      </div>
    </section>
  );
}
