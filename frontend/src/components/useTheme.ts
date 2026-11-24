"use client";

import { useCallback, useEffect, useState } from "react";
import { THEME_STORAGE_KEY, isThemeChoice, resolveTheme, type ThemeChoice } from "@/lib/theme";

function readChoice(): ThemeChoice {
  try {
    const stored = localStorage.getItem(THEME_STORAGE_KEY);
    return isThemeChoice(stored) ? stored : "system";
  } catch {
    return "system";
  }
}

function apply(choice: ThemeChoice) {
  const dark = matchMedia("(prefers-color-scheme: dark)").matches;
  document.documentElement.dataset.theme = resolveTheme(choice, dark);
}

export function useTheme() {
  const [choice, setChoice] = useState<ThemeChoice>("system");

  useEffect(() => {
    setChoice(readChoice());
  }, []);

  useEffect(() => {
    if (choice !== "system") return;

    const query = matchMedia("(prefers-color-scheme: dark)");
    const listener = () => apply("system");
    query.addEventListener("change", listener);

    return () => query.removeEventListener("change", listener);
  }, [choice]);

  const select = useCallback((next: ThemeChoice) => {
    setChoice(next);
    apply(next);

    try {
      localStorage.setItem(THEME_STORAGE_KEY, next);
    } catch {
      /* storage unavailable: the choice still applies for this visit */
    }
  }, []);

  return { choice, select };
}
