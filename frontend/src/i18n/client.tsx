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
  "invalid",
  "generic",
] as const;

export function errorMessage(t: Translate, code: string) {
  const known = (KNOWN_ERRORS as readonly string[]).includes(code) ? code : "generic";

  return t(`errors.${known}` as MessageKey);
}
