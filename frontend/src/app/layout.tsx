import type { Metadata } from "next";
import type { ReactNode } from "react";
import { I18nProvider } from "@/i18n/client";
import { messagesFor } from "@/i18n/core";
import { getLocale, getT } from "@/i18n/server";
import "./globals.css";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();

  return {
    title: { default: "Prumo", template: "%s | Prumo" },
    description: t("meta.description"),
  };
}

export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getLocale();

  return (
    <html lang={locale === "pt" ? "pt-BR" : "en"}>
      <body suppressHydrationWarning>
        <I18nProvider locale={locale} messages={messagesFor(locale)}>
          {children}
        </I18nProvider>
      </body>
    </html>
  );
}
