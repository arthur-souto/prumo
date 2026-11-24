"use client";

import { ArrowLeft, GitBranch, Trash2 } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { Markdown } from "@/components/ui/Markdown";
import { Select } from "@/components/ui/Select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/Tabs";
import { TextArea } from "@/components/ui/TextArea";
import { TextField } from "@/components/ui/TextField";
import { useToast } from "@/components/ui/Toast";
import { errorMessage, useI18n } from "@/i18n/client";
import { SUBTASK_STATUSES, type Repository, type Subtask, type SubtaskStatus } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { BranchLine, PullRequestList } from "../../board/Subtasks";
import styles from "./subtask.module.css";

type SubtaskViewProps = {
  workspaceId: string;
  initial: Subtask;
  storyTitle: string;
  repository: Repository | undefined;
  canBranch: boolean;
};

export function SubtaskView({ workspaceId, initial, storyTitle, repository, canBranch }: SubtaskViewProps) {
  const { t } = useI18n();
  const toast = useToast();
  const router = useRouter();
  const base = `/api/workspaces/${workspaceId}/subtasks/${initial.key}`;
  const [subtask, setSubtask] = useState(initial);
  const [title, setTitle] = useState(initial.title);
  const [description, setDescription] = useState(initial.description ?? "");
  const [branch, setBranch] = useState("");
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const dirty = title !== subtask.title || description !== (subtask.description ?? "");
  const boardHref = `/workspaces/${workspaceId}/board`;

  function fail(code: string, reason?: string) {
    setError(errorMessage(t, code, reason));
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(undefined);
    setFields({});
    const result = await sendJson<Subtask>(base, "PUT", { title, description });
    setSaving(false);

    if (result.ok && result.data) {
      setSubtask(result.data);
      toast({ tone: "success", title: t("story.saved") });
      return;
    }

    setFields(result.fields);
    if (Object.keys(result.fields).length === 0) fail(result.code, result.reason);
  }

  async function changeStatus(status: SubtaskStatus) {
    const result = await sendJson<Subtask>(`${base}/status`, "PUT", { status });

    if (result.ok && result.data) setSubtask(result.data);
    else fail(result.code, result.reason);
  }

  async function createBranch() {
    setBusy(true);
    setError(undefined);
    let name = branch.trim();

    if (name === "") {
      const suggestion = await sendJson<{ name: string }>(`${base}/branch-suggestion`, "GET");
      name = suggestion.data?.name ?? "";
    }

    const result = await sendJson<Subtask>(`${base}/branch`, "POST", { name });
    setBusy(false);

    if (result.ok && result.data) {
      setSubtask(result.data);
      toast({ tone: "success", title: t("story.branchCreated") });
    } else {
      fail(result.code, result.reason);
    }
  }

  async function remove() {
    setDeleting(true);
    const result = await sendJson(base, "DELETE");
    setDeleting(false);

    if (result.ok) {
      router.push(boardHref);
      router.refresh();
      return;
    }

    setConfirmDelete(false);
    fail(result.code, result.reason);
  }

  return (
    <div className={styles.page}>
      <Link className={styles.back} href={boardHref}>
        <ArrowLeft size={14} aria-hidden="true" />
        {t("story.backTo", { key: subtask.storyKey })}
      </Link>
      <p className={styles.crumb}>
        {t("story.subtaskOf", { key: subtask.storyKey })} · {storyTitle}
      </p>

      <form className={styles.layout} onSubmit={save}>
        <div className={styles.main}>
          {error ? <Alert>{error}</Alert> : null}
          <span className={styles.key}>{subtask.key}</span>
          <TextField label={t("story.title")} hideLabel value={title} maxLength={300} error={fields.title} required onChange={(event) => setTitle(event.target.value)} />

          <section className={styles.section}>
            <h2 className={styles.heading}>{t("story.description")}</h2>
            <Tabs defaultValue="write">
              <TabsList aria-label={t("story.description")}>
                <TabsTrigger value="write">{t("story.write")}</TabsTrigger>
                <TabsTrigger value="preview">{t("story.preview")}</TabsTrigger>
              </TabsList>
              <TabsContent value="write">
                <TextArea
                  label={t("story.description")}
                  hideLabel
                  rows={8}
                  maxLength={20000}
                  value={description}
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

          <section className={styles.section}>
            <h2 className={styles.heading}>{t("story.pullRequests")}</h2>
            {subtask.pullRequests.length === 0 ? (
              <p className={styles.hint}>{t("story.noPullRequests", { key: subtask.key })}</p>
            ) : (
              <PullRequestList pullRequests={subtask.pullRequests} />
            )}
          </section>

          <div className={styles.actions}>
            <Button type="submit" appearance="primary" loading={saving} disabled={!dirty}>
              {t("story.save")}
            </Button>
          </div>
        </div>

        <aside className={styles.side}>
          <label className={styles.field}>
            <span>{t("story.status")}</span>
            <Select value={subtask.status} onChange={(event) => void changeStatus(event.target.value as SubtaskStatus)}>
              {SUBTASK_STATUSES.map((status) => (
                <option key={status} value={status}>
                  {t(`story.subtaskState.${status}`)}
                </option>
              ))}
            </Select>
          </label>

          <section className={styles.section}>
            <h2 className={styles.heading}>{t("story.branch")}</h2>
            {subtask.branchName ? (
              <BranchLine branch={subtask.branchName} repository={repository} />
            ) : canBranch ? (
              <div className={styles.stack}>
                <TextField label={t("story.branchName")} value={branch} placeholder="feature/PRU-13-…" onChange={(event) => setBranch(event.target.value)} />
                <Button type="button" size="sm" icon={<GitBranch size={14} aria-hidden="true" />} loading={busy} onClick={createBranch}>
                  {t("story.createBranch")}
                </Button>
              </div>
            ) : (
              <p className={styles.hint}>{t("story.createBranchNeedsRepository")}</p>
            )}
          </section>

          <Button type="button" appearance="subtle" size="sm" icon={<Trash2 size={14} aria-hidden="true" />} onClick={() => setConfirmDelete(true)}>
            {t("story.subtaskDelete")}
          </Button>
        </aside>
      </form>

      <ConfirmDialog
        open={confirmDelete}
        title={t("story.subtaskDeleteTitle", { key: subtask.key })}
        description={t("story.subtaskDeleteLead")}
        confirmLabel={t("story.subtaskDelete")}
        busy={deleting}
        onConfirm={remove}
        onCancel={() => setConfirmDelete(false)}
      />
    </div>
  );
}
