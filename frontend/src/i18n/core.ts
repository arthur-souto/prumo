import { en, type MessageKey, type Messages } from "./en";
import { pt } from "./pt";

export type Locale = "en" | "pt";

export const LOCALES: Locale[] = ["en", "pt"];
export const LANG_COOKIE = "prumo_lang";
export const DEFAULT_LOCALE: Locale = "en";

const catalogs: Record<Locale, Messages> = { en, pt };

export function isLocale(value: string | undefined | null): value is Locale {
  return value === "en" || value === "pt";
}

export function messagesFor(locale: Locale): Messages {
  return catalogs[locale];
}

export function resolveLocale(cookieValue: string | undefined, acceptLanguage: string | null): Locale {
  if (isLocale(cookieValue)) {
    return cookieValue;
  }

  return acceptLanguage?.toLowerCase().trim().startsWith("pt") ? "pt" : DEFAULT_LOCALE;
}

export function backendLanguage(locale: Locale) {
  return locale === "pt" ? "pt-BR" : "en";
}

export function dateLocale(locale: Locale) {
  return locale === "pt" ? "pt-BR" : "en-US";
}

export type Translate = (key: MessageKey, values?: Record<string, string | number>) => string;

export function createTranslator(messages: Messages): Translate {
  return (key, values) => {
    const found = key.split(".").reduce<unknown>((node, part) => (node as Record<string, unknown> | undefined)?.[part], messages);
    const text = typeof found === "string" ? found : key;

    return values ? text.replace(/\{(\w+)\}/g, (_, name) => String(values[name] ?? `{${name}}`)) : text;
  };
}
