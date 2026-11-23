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
import type { Workspace } from "@/lib/workspace-types";
import styles from "./modal-form.module.css";

type CreateWorkspaceModalProps = {
  open: boolean;
  onClose: () => void;
};

export function CreateWorkspaceModal({ open, onClose }: CreateWorkspaceModalProps) {
  const router = useRouter();
  const t = useT();
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  function close() {
    setError(undefined);
    setFields({});
    onClose();
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(undefined);
    setFields({});

    const result = await sendJson<Workspace>("/api/workspaces", "POST", {
      name: data.get("name"),
      description: data.get("description"),
    });

    if (result.ok && result.data) {
      router.push(`/workspaces/${result.data.id}`);
      router.refresh();
      return;
    }

    setFields(result.fields);
    setError(Object.keys(result.fields).length === 0 ? errorMessage(t, result.code) : undefined);
    setSubmitting(false);
  }

  return (
    <Modal open={open} title={t("createWorkspace.title")} onClose={close}>
      <p className={styles.lead}>{t("createWorkspace.lead")}</p>
      {error ? <Alert>{error}</Alert> : null}
      <form className={styles.form} onSubmit={onSubmit}>
        <TextField label={t("createWorkspace.name")} name="name" maxLength={100} placeholder={t("createWorkspace.namePlaceholder")} error={fields.name} required autoFocus />
        <TextArea label={t("createWorkspace.description")} name="description" maxLength={500} placeholder={t("createWorkspace.descriptionPlaceholder")} error={fields.description} />
        <div className={styles.actions}>
          <Button type="button" appearance="subtle" onClick={close}>
            {t("createWorkspace.cancel")}
          </Button>
          <Button type="submit" appearance="primary" loading={submitting}>
            {t("createWorkspace.submit")}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
