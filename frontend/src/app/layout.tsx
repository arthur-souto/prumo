import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Providers } from "@/components/Providers";
import { I18nProvider } from "@/i18n/client";
import { messagesFor } from "@/i18n/core";
import { getLocale, getT } from "@/i18n/server";
import { THEME_INIT_SCRIPT } from "@/lib/theme";
import "./globals.css";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();

  return {
    title: { default: "Prumo", template: "%s | Prumo" },
    description: t("meta.description"),
    icons: {
      icon: [
        { url: "/favicon.ico", sizes: "16x16 32x32 48x48" },
        // the light icon in both themes: it has its own background, so it stands out on light and dark tabs alike
        { url: "/icons/icon.svg", type: "image/svg+xml" },
      ],
      apple: "/icons/apple-touch-icon.png",
    },
  };
}

export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getLocale();

  return (
    <html lang={locale === "pt" ? "pt-BR" : "en"} suppressHydrationWarning>
      <head>
        <script dangerouslySetInnerHTML={{ __html: THEME_INIT_SCRIPT }} />
      </head>
      <body suppressHydrationWarning>
        <I18nProvider locale={locale} messages={messagesFor(locale)}>
          <Providers>{children}</Providers>
        </I18nProvider>
      </body>
    </html>
  );
}
