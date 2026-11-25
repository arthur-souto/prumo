"use client";

import { Button } from "@/components/ui/Button";
import { Modal } from "@/components/ui/Modal";
import { useT } from "@/i18n/client";
import formStyles from "../../../modal-form.module.css";

type SprintConfirmProps = {
  title: string;
  description: string;
  confirmLabel: string;
  tone: "primary" | "danger";
  busy: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function SprintConfirm({ title, description, confirmLabel, tone, busy, onConfirm, onCancel }: SprintConfirmProps) {
  const t = useT();

  return (
    <Modal open title={title} onClose={onCancel}>
      <p className={formStyles.lead}>{description}</p>
      <div className={formStyles.actions}>
        <Button appearance="subtle" onClick={onCancel}>
          {t("common.cancel")}
        </Button>
        <Button appearance={tone} loading={busy} onClick={onConfirm}>
          {confirmLabel}
        </Button>
      </div>
    </Modal>
  );
}
