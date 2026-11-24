"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { useT } from "@/i18n/client";
import { Button } from "./Button";
import styles from "./ConfirmDialog.module.css";

type ConfirmDialogProps = {
  open: boolean;
  title: string;
  description: string;
  confirmLabel: string;
  cancelLabel?: string;
  tone?: "danger" | "primary";
  busy?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
};

export function ConfirmDialog({ open, title, description, confirmLabel, cancelLabel, tone = "danger", busy, onConfirm, onCancel }: ConfirmDialogProps) {
  const t = useT();

  return (
    <Dialog.Root open={open} onOpenChange={(next) => (next ? undefined : onCancel())}>
      <Dialog.Portal>
        <Dialog.Overlay className={styles.scrim} />
        <Dialog.Content className={styles.dialog}>
          <Dialog.Title className={styles.title}>{title}</Dialog.Title>
          <Dialog.Description className={styles.description}>{description}</Dialog.Description>
          <div className={styles.actions}>
            <Button appearance="subtle" onClick={onCancel}>
              {cancelLabel ?? t("common.cancel")}
            </Button>
            <Button appearance={tone} loading={busy} onClick={onConfirm}>
              {confirmLabel}
            </Button>
          </div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
