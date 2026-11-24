"use client";

import { Check, LogOut, Monitor, Moon, Settings, Sun } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
import {
  DropdownContent,
  DropdownItem,
  DropdownLabel,
  DropdownMenu,
  DropdownSeparator,
  DropdownTrigger,
} from "@/components/ui/DropdownMenu";
import { useTheme } from "@/components/useTheme";
import { persistLocale, useI18n } from "@/i18n/client";
import { LOCALES } from "@/i18n/core";
import type { ThemeChoice } from "@/lib/theme";
import styles from "./shell.module.css";

const THEMES = [
  { value: "light", icon: Sun },
  { value: "dark", icon: Moon },
  { value: "system", icon: Monitor },
] as const satisfies readonly { value: ThemeChoice; icon: unknown }[];

export function UserMenu({ name, email }: { name: string; email: string }) {
  const router = useRouter();
  const { t, locale } = useI18n();
  const { choice, select } = useTheme();
  const [leaving, setLeaving] = useState(false);

  async function logout() {
    setLeaving(true);
    await fetch("/api/auth/logout", { method: "POST" }).catch(() => null);
    router.replace("/login");
    router.refresh();
  }

  return (
    <DropdownMenu>
      <DropdownTrigger>
        <button type="button" className={styles.avatarButton} aria-label={t("topbar.userMenu")} disabled={leaving}>
          <Avatar name={name} seed={email} size="sm" />
        </button>
      </DropdownTrigger>
      <DropdownContent>
        <div className={styles.identity}>
          <span className={styles.identityName}>{name}</span>
          <span className={styles.identityEmail}>{email}</span>
        </div>
        <DropdownSeparator />
        <DropdownLabel>{t("theme.label")}</DropdownLabel>
        {THEMES.map(({ value, icon: Icon }) => (
          <DropdownItem key={value} icon={choice === value ? <Check size={16} aria-hidden="true" /> : <Icon size={16} aria-hidden="true" />} onSelect={() => select(value)}>
            {t(`theme.${value}`)}
          </DropdownItem>
        ))}
        <DropdownSeparator />
        <DropdownLabel>{t("lang.label")}</DropdownLabel>
        {LOCALES.map((option) => (
          <DropdownItem
            key={option}
            icon={locale === option ? <Check size={16} aria-hidden="true" /> : <span className={styles.iconSpace} />}
            onSelect={() => {
              persistLocale(option);
              router.refresh();
            }}
          >
            {t(`lang.${option}`)}
          </DropdownItem>
        ))}
        <DropdownSeparator />
        <DropdownItem icon={<Settings size={16} aria-hidden="true" />} onSelect={() => router.push("/settings")}>
          {t("userMenu.settings")}
        </DropdownItem>
        <DropdownItem icon={<LogOut size={16} aria-hidden="true" />} onSelect={logout} danger>
          {t("nav.logout")}
        </DropdownItem>
      </DropdownContent>
    </DropdownMenu>
  );
}
