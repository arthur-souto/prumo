import { cookies, headers } from "next/headers";
import { LANG_COOKIE, createTranslator, messagesFor, resolveLocale, type Locale } from "./core";

export async function getLocale(): Promise<Locale> {
  const [cookieStore, headerStore] = await Promise.all([cookies(), headers()]);

  return resolveLocale(cookieStore.get(LANG_COOKIE)?.value, headerStore.get("accept-language"));
}

export async function getT() {
  const locale = await getLocale();

  return { locale, t: createTranslator(messagesFor(locale)) };
}
