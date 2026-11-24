"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { useToast } from "@/components/ui/Toast";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, useT } from "@/i18n/client";
import { sendJson } from "@/lib/client";
import styles from "./workspace.module.css";

type BoardSettingsProps = {
  workspaceId: string;
  keyPrefix: string;
  automationEnabled: boolean;
  hasStories: boolean;
  canEdit: boolean;
};

export function BoardSettings({ workspaceId, keyPrefix, automationEnabled, hasStories, canEdit }: BoardSettingsProps) {
  const router = useRouter();
  const t = useT();
  const [prefix, setPrefix] = useState(keyPrefix);
  const [automation, setAutomation] = useState(automationEnabled);
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [saving, setSaving] = useState(false);
  const toast = useToast();

  async function onSave(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError(undefined);
    setFields({});

    const result = await sendJson(`/api/workspaces/${workspaceId}/board/settings`, "PUT", {
      keyPrefix: hasStories ? undefined : prefix,
      automationEnabled: automation,
    });

    setSaving(false);

    if (result.ok) {
      toast({ tone: "success", title: t("boardSettings.saved") });
      router.refresh();
      return;
    }

    setFields(result.fields);
    setError(Object.keys(result.fields).length === 0 ? errorMessage(t, result.code, result.reason) : undefined);
  }

  return (
    <section className={styles.card}>
      <h2 className={styles.cardTitle}>{t("boardSettings.title")}</h2>
      <form className={styles.form} onSubmit={onSave}>
        {error ? <Alert>{error}</Alert> : null}
        <TextField
          label={t("boardSettings.keyPrefix")}
          value={prefix}
          hint={t("boardSettings.keyPrefixHint")}
          error={fields.keyPrefix}
          maxLength={10}
          pattern="[A-Za-z][A-Za-z0-9]{1,9}"
          disabled={!canEdit || hasStories}
          onChange={(event) => setPrefix(event.target.value.toUpperCase())}
        />
        <label className={styles.check}>
          <input
            type="checkbox"
            checked={automation}
            disabled={!canEdit}
            onChange={(event) => setAutomation(event.target.checked)}
          />
          <span className={styles.checkText}>
            <span>{t("boardSettings.automation")}</span>
            <span className={styles.hint}>{t("boardSettings.automationHint")}</span>
          </span>
        </label>
        {canEdit ? (
          <div className={styles.actions}>
            <Button type="submit" appearance="primary" loading={saving}>
              {t("boardSettings.save")}
            </Button>
          </div>
        ) : null}
      </form>
    </section>
  );
}
