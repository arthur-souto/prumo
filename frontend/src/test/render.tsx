import { render, type RenderOptions } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import type { ReactElement, ReactNode } from "react";
import { Providers } from "@/components/Providers";
import { I18nProvider } from "@/i18n/client";
import { messagesFor, type Locale } from "@/i18n/core";

type Options = Omit<RenderOptions, "wrapper"> & { locale?: Locale };

/**
 * Renders inside the same providers as the app (i18n, toasts, tooltips) and returns a user-event session.
 * Defaults to English so assertions read like the UI copy in en.ts.
 */
export function renderWithProviders(ui: ReactElement, { locale = "en", ...options }: Options = {}) {
  function Wrapper({ children }: { children: ReactNode }) {
    return (
      <I18nProvider locale={locale} messages={messagesFor(locale)}>
        <Providers>{children}</Providers>
      </I18nProvider>
    );
  }

  return { user: userEvent.setup(), ...render(ui, { wrapper: Wrapper, ...options }) };
}
