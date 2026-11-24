"use client";

import * as Menu from "@radix-ui/react-dropdown-menu";
import type { ReactNode } from "react";
import styles from "./DropdownMenu.module.css";

export const DropdownMenu = Menu.Root;

export function DropdownTrigger({ children }: { children: ReactNode }) {
  return <Menu.Trigger asChild>{children}</Menu.Trigger>;
}

export function DropdownContent({ children, align = "end" }: { children: ReactNode; align?: "start" | "center" | "end" }) {
  return (
    <Menu.Portal>
      <Menu.Content className={styles.content} align={align} sideOffset={4}>
        {children}
      </Menu.Content>
    </Menu.Portal>
  );
}

type DropdownItemProps = {
  icon?: ReactNode;
  children: ReactNode;
  onSelect?: () => void;
  disabled?: boolean;
  danger?: boolean;
};

export function DropdownItem({ icon, children, onSelect, disabled, danger }: DropdownItemProps) {
  return (
    <Menu.Item className={`${styles.item} ${danger ? styles.danger : ""}`} onSelect={onSelect} disabled={disabled}>
      {icon ? <span className={styles.icon}>{icon}</span> : null}
      <span>{children}</span>
    </Menu.Item>
  );
}

export function DropdownLabel({ children }: { children: ReactNode }) {
  return <Menu.Label className={styles.label}>{children}</Menu.Label>;
}

export function DropdownSeparator() {
  return <Menu.Separator className={styles.separator} />;
}
