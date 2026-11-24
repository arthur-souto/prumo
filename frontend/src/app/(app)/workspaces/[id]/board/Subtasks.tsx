"use client";

import { Copy, ExternalLink, GitBranch, GitMerge, GitPullRequest, Plus, Trash2 } from "lucide-react";
import Link from "next/link";
import { useCallback, useEffect, useState, type KeyboardEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { IconButton } from "@/components/ui/IconButton";
import { Select } from "@/components/ui/Select";
import { GithubOrigin } from "./GithubOrigin";
import { TextField } from "@/components/ui/TextField";
import { useToast } from "@/components/ui/Toast";
import { errorMessage, useI18n } from "@/i18n/client";
import { SUBTASK_STATUSES, type PullRequestState, type Repository, type Story, type Subtask, type SubtaskStatus } from "@/lib/board-types";
import { isHttpUrl } from "@/lib/board-utils";
import { sendJson } from "@/lib/client";
import styles from "./Subtasks.module.css";

type SubtasksProps = {
  workspaceId: string;
  story: Story;
  repository: Repository | undefined;
  onProgress: (progress: { total: number; done: number }) => void;
};

const PR_TONE: Record<PullRequestState, BadgeTone> = { MERGED: "success", OPEN: "info", CLOSED: "neutral" };
const PR_ICON = { MERGED: GitMerge, OPEN: GitPullRequest, CLOSED: GitPullRequest } as const;

export function summarize(subtasks: Subtask[]) {
  return { total: subtasks.length, done: subtasks.filter((subtask) => subtask.status === "DONE").length };
}

export function BranchLine({ branch, repository }: { branch: string; repository: Repository | undefined }) {
  const { t } = useI18n();
  const toast = useToast();
  const href = repository && isHttpUrl(repository.htmlUrl) ? `${repository.htmlUrl}/tree/${branch}` : undefined;

  async function copy() {
    try {
      await navigator.clipboard.writeText(branch);
      toast({ tone: "success", title: t("story.branchCopied") });
    } catch {
      /* clipboard unavailable */
    }
  }

  return (
    <span className={styles.branch}>
      <GitBranch size={14} aria-hidden="true" />
      {href ? (
        <a href={href} target="_blank" rel="noopener noreferrer">
          {branch}
        </a>
      ) : (
        <span>{branch}</span>
      )}
      <IconButton size="sm" label={t("story.copyBranch")} icon={<Copy size={14} aria-hidden="true" />} onClick={copy} />
    </span>
  );
}

export function PullRequestList({ pullRequests }: { pullRequests: Subtask["pullRequests"] }) {
  const { t } = useI18n();

  return (
    <ul className={styles.prList}>
      {pullRequests.map((pullRequest) => {
        const Icon = PR_ICON[pullRequest.state];

        return (
          <li key={`${pullRequest.repositoryId}-${pullRequest.number}`} className={styles.pr}>
            <Icon size={14} aria-hidden="true" />
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
            <Badge tone={PR_TONE[pullRequest.state]}>{t(`story.prState.${pullRequest.state}`)}</Badge>
          </li>
        );
      })}
    </ul>
  );
}

export function Subtasks({ workspaceId, story, repository, onProgress }: SubtasksProps) {
  const { t } = useI18n();
  const toast = useToast();
  const base = `/api/workspaces/${workspaceId}`;
  const [items, setItems] = useState<Subtask[] | null>(null);
  const [error, setError] = useState<string>();
  const [title, setTitle] = useState("");
  const [withBranch, setWithBranch] = useState(false);
  const [branchName, setBranchName] = useState("");
  const [creating, setCreating] = useState(false);
  const [toDelete, setToDelete] = useState<Subtask | null>(null);
  const [deleting, setDeleting] = useState(false);
  const canBranch = story.repositoryId !== null;

  const update = useCallback(
    (next: Subtask[]) => {
      setItems(next);
      onProgress(summarize(next));
    },
    [onProgress],
  );

  useEffect(() => {
    let cancelled = false;

    sendJson<Subtask[]>(`${base}/stories/${story.key}/subtasks`, "GET").then((result) => {
      if (cancelled) return;
      if (result.ok && result.data) setItems(result.data);
      else setError(errorMessage(t, result.code, result.reason));
    });

    return () => {
      cancelled = true;
    };
    // loaded once per story; later changes are applied locally
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [base, story.key]);

  async function create() {
    const text = title.trim();
    if (text === "" || items === null) return;

    setCreating(true);
    setError(undefined);
    const created = await sendJson<Subtask>(`${base}/stories/${story.key}/subtasks`, "POST", { title: text });

    if (!created.ok || !created.data) {
      setCreating(false);
      setError(errorMessage(t, created.code, created.reason));
      return;
    }

    let subtask = created.data;

    if (withBranch && canBranch) {
      // the key only exists now, so the suggested name is asked for after creating
      const typed = branchName.trim();
      const suggestion = typed === "" ? await sendJson<{ name: string }>(`${base}/subtasks/${subtask.key}/branch-suggestion`, "GET") : null;
      const name = typed !== "" ? typed : suggestion?.data?.name;
      const branch = name
        ? await sendJson<Subtask>(`${base}/subtasks/${subtask.key}/branch`, "POST", { name })
        : null;

      if (branch?.ok && branch.data) {
        subtask = branch.data;
      } else {
        setError(t("story.subtaskBranchFailed", { key: subtask.key, reason: errorMessage(t, branch?.code ?? "generic", branch?.reason) }));
      }
    } else {
      toast({ tone: "success", title: t("story.subtaskCreated", { key: subtask.key }) });
    }

    update([...items, subtask]);
    setTitle("");
    setBranchName("");
    setCreating(false);
  }

  async function changeStatus(subtask: Subtask, status: SubtaskStatus) {
    const result = await sendJson<Subtask>(`${base}/subtasks/${subtask.key}/status`, "PUT", { status });

    if (!result.ok || !result.data || items === null) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    update(items.map((candidate) => (candidate.id === subtask.id ? result.data! : candidate)));
  }

  async function remove() {
    if (!toDelete || items === null) return;

    setDeleting(true);
    const result = await sendJson(`${base}/subtasks/${toDelete.key}`, "DELETE");
    setDeleting(false);

    if (!result.ok) {
      setError(errorMessage(t, result.code, result.reason));
    } else {
      update(items.filter((candidate) => candidate.id !== toDelete.id));
    }

    setToDelete(null);
  }

  // Not a <form>: this section lives inside the story form, so Enter is handled here instead.
  function onAddKeyDown(event: KeyboardEvent<HTMLDivElement>) {
    if (event.key === "Enter" && !event.shiftKey && !event.metaKey && !event.ctrlKey && event.target instanceof HTMLInputElement && event.target.type === "text") {
      event.preventDefault();
      event.stopPropagation();
      void create();
    }
  }

  const progress = items ? summarize(items) : null;

  return (
    <section className={styles.section}>
      <h3 className={styles.title}>
        {t("story.subtasks")}
        {progress && progress.total > 0 ? (
          <span className={styles.count}>
            {progress.done}/{progress.total}
          </span>
        ) : null}
      </h3>
      {progress && progress.total > 0 ? (
        <div className={styles.bar} role="progressbar" aria-valuemin={0} aria-valuemax={progress.total} aria-valuenow={progress.done} aria-label={t("story.subtasks")}>
          <span style={{ width: `${(progress.done / progress.total) * 100}%` }} />
        </div>
      ) : null}
      {error ? <Alert>{error}</Alert> : null}

      {items !== null && items.length === 0 ? <p className={styles.hint}>{t("story.subtaskNone")}</p> : null}
      <ul className={styles.list}>
        {(items ?? []).map((subtask) => (
          <li key={subtask.id} className={styles.item}>
            <div className={styles.row}>
              <Link className={styles.key} href={`/workspaces/${workspaceId}/subtasks/${subtask.key}`}>
                {subtask.key}
              </Link>
              <Link className={`${styles.name} ${subtask.status === "DONE" ? styles.done : ""}`} href={`/workspaces/${workspaceId}/subtasks/${subtask.key}`}>
                {subtask.title}
              </Link>
              {subtask.statusSource === "GITHUB" ? <GithubOrigin action="status" /> : null}
              <Select
                compact
                variant="quiet"
                aria-label={t("story.subtaskStatus")}
                value={subtask.status}
                onChange={(event) => void changeStatus(subtask, event.target.value as SubtaskStatus)}
              >
                {SUBTASK_STATUSES.map((status) => (
                  <option key={status} value={status}>
                    {t(`story.subtaskState.${status}`)}
                  </option>
                ))}
              </Select>
              <IconButton size="sm" label={t("story.subtaskDelete")} icon={<Trash2 size={14} aria-hidden="true" />} onClick={() => setToDelete(subtask)} />
            </div>
            {subtask.branchName || subtask.pullRequests.length > 0 ? (
              <div className={styles.extras}>
                {subtask.branchName ? <BranchLine branch={subtask.branchName} repository={repository} /> : null}
                {subtask.pullRequests.length > 0 ? <PullRequestList pullRequests={subtask.pullRequests} /> : null}
              </div>
            ) : null}
          </li>
        ))}
      </ul>

      <div className={styles.form} onKeyDown={onAddKeyDown}>
        <div className={styles.addRow}>
          <TextField
            label={t("story.addSubtask")}
            hideLabel
            value={title}
            maxLength={300}
            placeholder={t("story.subtaskTitle")}
            onChange={(event) => setTitle(event.target.value)}
          />
          <Button type="button" size="sm" icon={<Plus size={14} aria-hidden="true" />} loading={creating} disabled={title.trim() === ""} onClick={() => void create()}>
            {t("story.addSubtask")}
          </Button>
        </div>
        <label className={styles.check}>
          <input type="checkbox" checked={withBranch && canBranch} disabled={!canBranch} onChange={(event) => setWithBranch(event.target.checked)} />
          <span>{t("story.createBranchWithSubtask")}</span>
        </label>
        {!canBranch ? <p className={styles.hint}>{t("story.createBranchNeedsRepository")}</p> : null}
        {withBranch && canBranch ? (
          <TextField
            label={t("story.branchName")}
            value={branchName}
            hint={t("story.branchSuggested")}
            placeholder="feature/PRU-13-…"
            maxLength={200}
            onChange={(event) => setBranchName(event.target.value)}
          />
        ) : null}
      </div>

      <ConfirmDialog
        open={toDelete !== null}
        title={t("story.subtaskDeleteTitle", { key: toDelete?.key ?? "" })}
        description={t("story.subtaskDeleteLead")}
        confirmLabel={t("story.subtaskDelete")}
        busy={deleting}
        onConfirm={remove}
        onCancel={() => setToDelete(null)}
      />
    </section>
  );
}
