"use client";

import * as Menu from "@radix-ui/react-dropdown-menu";
import { Check, ChevronDown } from "lucide-react";
import type { ReactNode } from "react";
import styles from "./FilterMenu.module.css";

export type FilterOption = {
  value: string;
  label: string;
  icon?: ReactNode;
};

type FilterMenuProps = {
  label: string;
  options: FilterOption[];
  selected: string[];
  onToggle: (value: string) => void;
  onClear: () => void;
  clearLabel: string;
  emptyLabel?: string;
};

export function FilterMenu({ label, options, selected, onToggle, onClear, clearLabel, emptyLabel }: FilterMenuProps) {
  const count = selected.length;

  return (
    <Menu.Root modal={false}>
      <Menu.Trigger asChild>
        <button type="button" className={`${styles.trigger} ${count > 0 ? styles.active : ""}`}>
          <span>{label}</span>
          {count > 0 ? <span className={styles.count}>{count}</span> : null}
          <ChevronDown size={14} aria-hidden="true" className={styles.chevron} />
        </button>
      </Menu.Trigger>
      <Menu.Portal>
        <Menu.Content className={styles.content} align="start" sideOffset={4}>
          {options.length === 0 && emptyLabel ? <p className={styles.empty}>{emptyLabel}</p> : null}
          {options.map((option) => (
            <Menu.CheckboxItem
              key={option.value}
              className={styles.item}
              checked={selected.includes(option.value)}
              onCheckedChange={() => onToggle(option.value)}
              onSelect={(event) => event.preventDefault()}
            >
              <span className={styles.box} aria-hidden="true">
                <Menu.ItemIndicator>
                  <Check size={12} />
                </Menu.ItemIndicator>
              </span>
              {option.icon}
              <span className={styles.text}>{option.label}</span>
            </Menu.CheckboxItem>
          ))}
          {count > 0 ? (
            <>
              <Menu.Separator className={styles.separator} />
              <Menu.Item className={styles.item} onSelect={onClear}>
                {clearLabel}
              </Menu.Item>
            </>
          ) : null}
        </Menu.Content>
      </Menu.Portal>
    </Menu.Root>
  );
}
