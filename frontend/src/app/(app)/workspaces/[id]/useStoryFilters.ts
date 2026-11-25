"use client";

import { useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useState } from "react";
import { readStoryFilters, writeStoryFilters, type StoryFilters } from "@/lib/story-filters";
import { replaceSearchParams } from "@/lib/url-state";
import { useDebouncedValue } from "@/lib/use-debounced-value";

const TEXT_DELAY = 300;

function sameIgnoringText(a: StoryFilters, b: StoryFilters) {
  return JSON.stringify({ ...a, text: "" }) === JSON.stringify({ ...b, text: "" });
}

export function useStoryFilters() {
  const params = useSearchParams();
  const applied = useMemo(() => readStoryFilters(new URLSearchParams(params.toString())), [params]);
  const [text, setText] = useState(applied.text);
  const [syncedText, setSyncedText] = useState(applied.text);
  const typed = useDebouncedValue(text, TEXT_DELAY);

  if (applied.text !== syncedText) {
    setSyncedText(applied.text);
    setText(applied.text);
  }

  useEffect(() => {
    const value = typed.trim();

    if (typed !== text || value === applied.text) return;

    setSyncedText(value);
    replaceSearchParams((current) => writeStoryFilters(current, { ...applied, text: value }));
  }, [typed, text, applied]);

  const setFilters = useCallback(
    (next: StoryFilters) => {
      setText(next.text);

      const cleared = next.text.trim() === "" && applied.text !== "";

      if (sameIgnoringText(next, applied) && !cleared) return;

      setSyncedText(next.text.trim());
      replaceSearchParams((current) => writeStoryFilters(current, next));
    },
    [applied],
  );

  const filters = useMemo(() => ({ ...applied, text }), [applied, text]);

  return { filters, applied, setFilters };
}
