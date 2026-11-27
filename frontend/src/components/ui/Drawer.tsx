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
  /**
   * What the panel is, read by screen readers before the visible title: "Story details: PRU-2".
   * Radix names the dialog after its title, so this lives inside the title rather than in an aria-label.
   */
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
        <Dialog.Content
          className={`${styles.panel} ${styles[width]}`}
          aria-describedby={undefined}
          tabIndex={-1}
          onOpenAutoFocus={(event) => {
            event.preventDefault();
            (event.currentTarget as HTMLElement).focus();
          }}
        >
          <header className={styles.header}>
            <Dialog.Title asChild>
              <div className={styles.title}>
                {/* the space stays outside the hidden span, or the accessible name reads "Story details:PRU-2" */}
                <span className={styles.visuallyHidden}>{`${label}:`}</span> {title}
              </div>
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
