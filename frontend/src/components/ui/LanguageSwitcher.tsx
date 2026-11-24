"use client";

import { useRouter } from "next/navigation";
import { persistLocale, useI18n } from "@/i18n/client";
import { LOCALES, isLocale } from "@/i18n/core";
import { Select } from "./Select";

export function LanguageSwitcher() {
  const router = useRouter();
  const { locale, t } = useI18n();

  return (
    <Select
      compact
      aria-label={t("lang.label")}
      value={locale}
      onChange={(event) => {
        if (isLocale(event.target.value)) {
          persistLocale(event.target.value);
          router.refresh();
        }
      }}
    >
      {LOCALES.map((option) => (
        <option key={option} value={option}>
          {t(`lang.${option}`)}
        </option>
      ))}
    </Select>
  );
}
