"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { TextArea } from "@/components/ui/TextArea";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, useT } from "@/i18n/client";
import type { Sprint } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import formStyles from "../../../modal-form.module.css";
import styles from "./sprints.module.css";

type SprintFormModalProps = {
  workspaceId: string;
  sprint: Sprint | null;
  suggestedName: string;
  suggestedStart: string;
  onSaved: (sprint: Sprint) => void;
  onClose: () => void;
};

const SPRINT_DAYS = 14;

export function addDays(date: string, days: number) {
  const next = new Date(`${date}T00:00:00Z`);
  next.setUTCDate(next.getUTCDate() + days);
  return next.toISOString().slice(0, 10);
}

export function SprintFormModal({ workspaceId, sprint, suggestedName, suggestedStart, onSaved, onClose }: SprintFormModalProps) {
  const t = useT();
  const [name, setName] = useState(sprint?.name ?? suggestedName);
  const [goal, setGoal] = useState(sprint?.goal ?? "");
  const [startDate, setStartDate] = useState(sprint?.startDate ?? suggestedStart);
  const [endDate, setEndDate] = useState(sprint?.endDate ?? addDays(suggestedStart, SPRINT_DAYS - 1));
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  const endBeforeStart = startDate !== "" && endDate !== "" && endDate < startDate;

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (endBeforeStart) return;

    setSubmitting(true);
    setError(undefined);
    setFields({});

    const url = sprint ? `/api/workspaces/${workspaceId}/sprints/${sprint.id}` : `/api/workspaces/${workspaceId}/sprints`;
    const result = await sendJson<Sprint>(url, sprint ? "PUT" : "POST", { name, goal, startDate, endDate });

    setSubmitting(false);

    if (result.ok && result.data) {
      onSaved(result.data);
      return;
    }

    setFields(result.fields);
    setError(Object.keys(result.fields).length === 0 ? errorMessage(t, result.code) : undefined);
  }

  return (
    <Modal open title={sprint ? t("sprints.editTitle") : t("sprints.createTitle")} onClose={onClose}>
      {error ? <Alert>{error}</Alert> : null}
      <form className={formStyles.form} onSubmit={onSubmit}>
        <TextField
          label={t("sprints.name")}
          value={name}
          maxLength={80}
          placeholder={t("sprints.namePlaceholder")}
          error={fields.name}
          onChange={(event) => setName(event.target.value)}
          required
          autoFocus
        />
        <TextArea
          label={t("sprints.goal")}
          value={goal}
          maxLength={500}
          placeholder={t("sprints.goalPlaceholder")}
          error={fields.goal}
          onChange={(event) => setGoal(event.target.value)}
        />
        <div className={styles.dateFields}>
          <TextField
            label={t("sprints.startDate")}
            type="date"
            value={startDate}
            error={fields.startDate}
            onChange={(event) => setStartDate(event.target.value)}
            required
          />
          <TextField
            label={t("sprints.endDate")}
            type="date"
            value={endDate}
            min={startDate || undefined}
            error={endBeforeStart || fields.endOnOrAfterStart ? t("sprints.endBeforeStart") : fields.endDate}
            onChange={(event) => setEndDate(event.target.value)}
            required
          />
        </div>
        <div className={formStyles.actions}>
          <Button type="button" appearance="subtle" onClick={onClose}>
            {t("common.cancel")}
          </Button>
          <Button type="submit" appearance="primary" loading={submitting} disabled={endBeforeStart}>
            {sprint ? t("sprints.save") : t("sprints.create")}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
