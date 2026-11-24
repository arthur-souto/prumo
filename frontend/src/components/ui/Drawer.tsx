"use client";

import * as Dialog from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import type { ReactNode } from "react";
import { useT } from "@/i18n/client";
import { IconButton } from "./IconButton";
import styles from "./Drawer.module.css";

type DrawerProps = {
  open: boolean;
  onClose: () => void;
  title: ReactNode;
  /** Accessible name when the visible title is not plain text. */
  label: string;
  children: ReactNode;
  width?: "md" | "lg" | "dialog";
  /** Buttons shown next to the close button. */
  headerActions?: ReactNode;
};

export function Drawer({ open, onClose, title, label, children, width = "md", headerActions }: DrawerProps) {
  const t = useT();

  return (
    <Dialog.Root open={open} onOpenChange={(next) => (next ? undefined : onClose())}>
      <Dialog.Portal>
        <Dialog.Overlay className={styles.scrim} />
        <Dialog.Content className={`${styles.panel} ${styles[width]}`} aria-label={label} aria-describedby={undefined}>
          <header className={styles.header}>
            <Dialog.Title asChild>
              <div className={styles.title}>{title}</div>
            </Dialog.Title>
            <div className={styles.headerActions}>
              {headerActions}
            <Dialog.Close asChild>
              <IconButton label={t("common.close")} icon={<X size={18} aria-hidden="true" />} size="sm" />
            </Dialog.Close>
            </div>
          </header>
          <div className={styles.body}>{children}</div>
        </Dialog.Content>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
