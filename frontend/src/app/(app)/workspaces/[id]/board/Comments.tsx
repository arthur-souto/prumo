"use client";

import { Pencil, Trash2 } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { ConfirmDialog } from "@/components/ui/ConfirmDialog";
import { IconButton } from "@/components/ui/IconButton";
import { Markdown } from "@/components/ui/Markdown";
import { TextArea } from "@/components/ui/TextArea";
import { useToast } from "@/components/ui/Toast";
import { errorMessage, useI18n } from "@/i18n/client";
import { sendJson } from "@/lib/client";
import type { Comment } from "@/lib/board-types";
import styles from "./comments.module.css";

const MAX_LENGTH = 20000;

type CommentsProps = {
  workspaceId: string;
  /** "stories/PRU-1" for the comments of a story, or "subtasks/PRU-2" for those of one subtask. */
  scope: string;
};

export function Comments({ workspaceId, scope }: CommentsProps) {
  const { t, locale } = useI18n();
  const toast = useToast();
  const base = `/api/workspaces/${workspaceId}`;
  const [comments, setComments] = useState<Comment[] | null>(null);
  const [failed, setFailed] = useState(false);
  const [draft, setDraft] = useState("");
  const [posting, setPosting] = useState(false);
  const [error, setError] = useState("");
  const [editing, setEditing] = useState<string | null>(null);
  const [editText, setEditText] = useState("");
  const [saving, setSaving] = useState(false);
  const [editError, setEditError] = useState("");
  const [toDelete, setToDelete] = useState<Comment | null>(null);
  const [deleting, setDeleting] = useState(false);

  const load = useCallback(async () => {
    const result = await sendJson<Comment[]>(`${base}/${scope}/comments`, "GET");
    setFailed(!result.ok);
    if (result.ok && result.data) setComments(result.data);
  }, [base, scope]);

  useEffect(() => {
    setComments(null);
    setEditing(null);
    void load();
  }, [load]);

  async function post() {
    const body = draft.trim();
    if (!body) return;
    if (body.length > MAX_LENGTH) {
      setError(t("comments.tooLong"));
      return;
    }

    setPosting(true);
    setError("");
    const result = await sendJson<Comment>(`${base}/${scope}/comments`, "POST", { body });
    setPosting(false);

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    setComments((current) => [...(current ?? []), result.data as Comment]);
    setDraft("");
    toast({ tone: "success", title: t("comments.added") });
  }

  function startEditing(comment: Comment) {
    setEditing(comment.id);
    setEditText(comment.body);
    setEditError("");
  }

  async function saveEdit(comment: Comment) {
    const body = editText.trim();
    if (!body) return;
    if (body.length > MAX_LENGTH) {
      setEditError(t("comments.tooLong"));
      return;
    }

    setSaving(true);
    const result = await sendJson<Comment>(`${base}/comments/${comment.id}`, "PUT", { body });
    setSaving(false);

    if (!result.ok || !result.data) {
      setEditError(errorMessage(t, result.code, result.reason));
      return;
    }

    setComments((current) => (current ?? []).map((item) => (item.id === comment.id ? (result.data as Comment) : item)));
    setEditing(null);
  }

  async function remove() {
    if (!toDelete) return;

    setDeleting(true);
    const result = await sendJson<null>(`${base}/comments/${toDelete.id}`, "DELETE");
    setDeleting(false);

    if (!result.ok) {
      toast({ tone: "danger", title: errorMessage(t, result.code, result.reason) });
      setToDelete(null);
      return;
    }

    setComments((current) => (current ?? []).filter((item) => item.id !== toDelete.id));
    setToDelete(null);
    toast({ tone: "success", title: t("comments.deleted") });
  }

  function when(value: string) {
    return new Date(value).toLocaleString(locale, { dateStyle: "short", timeStyle: "short" });
  }

  return (
    <section className={styles.section}>
      <h3 className={styles.title}>
        {t("comments.title")}
        {comments && comments.length > 0 ? <span className={styles.count}>{comments.length}</span> : null}
      </h3>

      {failed && comments === null ? <p className={styles.hint}>{t("comments.failed")}</p> : null}
      {comments !== null && comments.length === 0 ? <p className={styles.hint}>{t("comments.empty")}</p> : null}

      <ul className={styles.list}>
        {(comments ?? []).map((comment) => {
          const name = comment.authorName ?? t("comments.removedUser");

          return (
            <li key={comment.id} className={styles.item}>
              <Avatar name={name} seed={comment.authorId ?? undefined} src={comment.authorAvatarUrl} size="sm" />
              <div className={styles.content}>
                <div className={styles.meta}>
                  <strong className={styles.author}>{name}</strong>
                  <time className={styles.time} dateTime={comment.createdAt}>
                    {when(comment.createdAt)}
                  </time>
                  {comment.editedAt ? (
                    <span className={styles.edited} title={when(comment.editedAt)}>
                      ({t("comments.edited")})
                    </span>
                  ) : null}
                  {comment.mine && editing !== comment.id ? (
                    <span className={styles.actions}>
                      <IconButton size="sm" label={t("comments.edit")} icon={<Pencil size={14} aria-hidden="true" />} onClick={() => startEditing(comment)} />
                      <IconButton size="sm" label={t("comments.delete")} icon={<Trash2 size={14} aria-hidden="true" />} onClick={() => setToDelete(comment)} />
                    </span>
                  ) : null}
                </div>

                {editing === comment.id ? (
                  <div className={styles.editor}>
                    <TextArea
                      label={t("comments.edit")}
                      hideLabel
                      rows={4}
                      value={editText}
                      error={editError}
                      onChange={(event) => setEditText(event.target.value)}
                    />
                    <div className={styles.editorActions}>
                      <Button type="button" size="sm" appearance="subtle" onClick={() => setEditing(null)}>
                        {t("common.cancel")}
                      </Button>
                      <Button type="button" size="sm" appearance="primary" loading={saving} disabled={!editText.trim()} onClick={() => void saveEdit(comment)}>
                        {t("comments.save")}
                      </Button>
                    </div>
                  </div>
                ) : (
                  <Markdown>{comment.body}</Markdown>
                )}
              </div>
            </li>
          );
        })}
      </ul>

      {/* Not a <form>: this block sits inside the story and subtask forms, and a nested submit would save them. */}
      <div className={styles.form}>
        <TextArea
          label={t("comments.label")}
          hideLabel
          rows={3}
          placeholder={t("comments.placeholder")}
          hint={t("comments.hint")}
          value={draft}
          error={error}
          onChange={(event) => setDraft(event.target.value)}
        />
        <div className={styles.formActions}>
          <Button type="button" appearance="primary" size="sm" loading={posting} disabled={!draft.trim()} onClick={() => void post()}>
            {t("comments.submit")}
          </Button>
        </div>
      </div>

      <ConfirmDialog
        open={toDelete !== null}
        title={t("comments.deleteTitle")}
        description={t("comments.deleteLead")}
        confirmLabel={t("comments.delete")}
        busy={deleting}
        onConfirm={() => void remove()}
        onCancel={() => setToDelete(null)}
      />
    </section>
  );
}
