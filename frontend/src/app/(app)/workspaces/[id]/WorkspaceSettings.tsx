"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { TextArea } from "@/components/ui/TextArea";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, useT } from "@/i18n/client";
import { sendJson } from "@/lib/client";
import modal from "../../modal-form.module.css";
import styles from "./workspace.module.css";

type WorkspaceSettingsProps = {
  id: string;
  name: string;
  description: string | null;
  canEdit: boolean;
  canDelete: boolean;
};

export function WorkspaceSettings({ id, name, description, canEdit, canDelete }: WorkspaceSettingsProps) {
  const router = useRouter();
  const t = useT();
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [confirmation, setConfirmation] = useState("");
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState<string>();

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSaving(true);
    setSaved(false);
    setError(undefined);
    setFields({});

    const result = await sendJson(`/api/workspaces/${id}`, "PUT", {
      name: data.get("name"),
      description: data.get("description"),
    });

    setSaving(false);

    if (result.ok) {
      setSaved(true);
      router.refresh();
      return;
    }

    setFields(result.fields);
    setError(Object.keys(result.fields).length === 0 ? errorMessage(t, result.code) : undefined);
  }

  async function onDelete() {
    setDeleting(true);
    setDeleteError(undefined);

    const result = await sendJson(`/api/workspaces/${id}`, "DELETE");

    if (result.ok) {
      router.replace("/");
      router.refresh();
      return;
    }

    setDeleteError(errorMessage(t, result.code));
    setDeleting(false);
  }

  if (!canEdit && !canDelete) {
    return null;
  }

  return (
    <div className={styles.form}>
      {canEdit ? (
        <section className={styles.card}>
          <h2 className={styles.cardTitle}>{t("workspace.settings")}</h2>
          {error ? <Alert>{error}</Alert> : null}
          <form className={styles.form} onSubmit={onSave} key={`${name}-${description}`}>
            <TextField label={t("workspace.name")} name="name" defaultValue={name} maxLength={100} error={fields.name} required />
            <TextArea label={t("workspace.description")} name="description" defaultValue={description ?? ""} maxLength={500} error={fields.description} />
            <div className={styles.actions}>
              {saved ? <span className={styles.saved}>{t("workspace.saved")}</span> : null}
              <Button type="submit" appearance="primary" loading={saving}>
                {t("workspace.save")}
              </Button>
            </div>
          </form>
        </section>
      ) : null}

      {canDelete ? (
        <section className={`${styles.card} ${styles.danger}`}>
          <h2 className={styles.cardTitle}>{t("workspace.deleteTitle")}</h2>
          <p className={styles.dangerText}>{t("workspace.deleteText")}</p>
          <Button appearance="danger" onClick={() => setConfirming(true)}>
            {t("workspace.deleteTitle")}
          </Button>
        </section>
      ) : null}

      <Modal open={confirming} title={t("workspace.deleteModalTitle", { name })} onClose={() => (deleting ? undefined : setConfirming(false))}>
        <p className={modal.lead}>{t("workspace.deleteLead")}</p>
        {deleteError ? <Alert>{deleteError}</Alert> : null}
        <TextField
          label={t("workspace.confirmLabel")}
          value={confirmation}
          onChange={(event) => setConfirmation(event.target.value)}
          placeholder={name}
          autoFocus
        />
        <div className={modal.actions}>
          <Button type="button" appearance="subtle" onClick={() => setConfirming(false)} disabled={deleting}>
            {t("workspace.cancel")}
          </Button>
          <Button type="button" appearance="danger" onClick={onDelete} loading={deleting} disabled={confirmation !== name}>
            {t("workspace.deleteTitle")}
          </Button>
        </div>
      </Modal>
    </div>
  );
}
