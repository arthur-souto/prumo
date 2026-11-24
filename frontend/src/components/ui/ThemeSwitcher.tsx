"use client";

import { Monitor, Moon, Sun } from "lucide-react";
import { useTheme } from "@/components/useTheme";
import { useT } from "@/i18n/client";
import styles from "./ThemeSwitcher.module.css";

const OPTIONS = [
  { value: "light", icon: Sun },
  { value: "dark", icon: Moon },
  { value: "system", icon: Monitor },
] as const;

export function ThemeSwitcher() {
  const t = useT();
  const { choice, select } = useTheme();

  return (
    <div className={styles.group} role="radiogroup" aria-label={t("theme.label")}>
      {OPTIONS.map(({ value, icon: Icon }) => (
        <button
          key={value}
          type="button"
          role="radio"
          aria-checked={choice === value}
          aria-label={t(`theme.${value}`)}
          title={t(`theme.${value}`)}
          className={styles.option}
          onClick={() => select(value)}
        >
          <Icon size={14} aria-hidden="true" />
        </button>
      ))}
    </div>
  );
}
