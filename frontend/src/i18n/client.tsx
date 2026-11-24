"use client";

import { createContext, useContext, useMemo, type ReactNode } from "react";
import { LANG_COOKIE, createTranslator, type Locale, type Translate } from "./core";
import type { MessageKey, Messages } from "./en";

type I18nValue = {
  locale: Locale;
  t: Translate;
};

const I18nContext = createContext<I18nValue | null>(null);

export function I18nProvider({ locale, messages, children }: { locale: Locale; messages: Messages; children: ReactNode }) {
  const value = useMemo(() => ({ locale, t: createTranslator(messages) }), [locale, messages]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useI18n() {
  const value = useContext(I18nContext);

  if (!value) {
    throw new Error("useI18n must be used inside I18nProvider");
  }

  return value;
}

export function useT() {
  return useI18n().t;
}

export function persistLocale(locale: Locale) {
  document.cookie = `${LANG_COOKIE}=${locale}; path=/; max-age=31536000; samesite=lax`;
}

const KNOWN_ERRORS = [
  "enterCredentials",
  "invalidCredentials",
  "emailTaken",
  "fixFields",
  "unavailable",
  "sessionExpired",
  "forbidden",
  "notFound",
  "conflict",
  "unprocessable",
  "invalid",
  "generic",
] as const;

const GITHUB_REASONS = [
  "MISSING_TOKEN",
  "INVALID_TOKEN",
  "PERMISSION_DENIED",
  "RATE_LIMITED",
  "NOT_FOUND",
  "BRANCH_EXISTS",
  "REPOSITORY_EXISTS",
  "UNAVAILABLE",
  "NOT_CONFIGURED",
  "UNEXPECTED",
] as const;

export function githubReason(t: Translate, reason: string) {
  return (GITHUB_REASONS as readonly string[]).includes(reason)
    ? t(`githubErrors.${reason}` as MessageKey)
    : t("errors.generic");
}

export function errorMessage(t: Translate, code: string, reason?: string) {
  if (reason && (GITHUB_REASONS as readonly string[]).includes(reason)) {
    return githubReason(t, reason);
  }

  const known = (KNOWN_ERRORS as readonly string[]).includes(code) ? code : "generic";

  return t(`errors.${known}` as MessageKey);
}
