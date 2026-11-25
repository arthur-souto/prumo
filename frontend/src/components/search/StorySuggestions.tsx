"use client";

import { useEffect, useId, useState, type KeyboardEvent } from "react";
import { PriorityBadge } from "@/app/(app)/workspaces/[id]/board/PriorityBadge";
import { useT } from "@/i18n/client";
import type { Story } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { EMPTY_STORY_FILTERS, storySearchQuery, type StoryPage } from "@/lib/story-filters";
import { useDebouncedValue } from "@/lib/use-debounced-value";
import styles from "./story-suggestions.module.css";

const MIN_LENGTH = 2;
const LIMIT = 6;
const DELAY = 200;

export function useStorySuggestions(workspaceId: string | undefined, value: string, onPick: ((story: Story) => void) | undefined) {
  const id = useId();
  const typed = value.trim();
  const term = useDebouncedValue(typed, DELAY);
  const enabled = Boolean(workspaceId && onPick);
  const [loaded, setLoaded] = useState<{ term: string; items: Story[] }>({ term: "", items: [] });
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  useEffect(() => {
    if (!enabled || term.length < MIN_LENGTH) return;

    let current = true;
    const query = storySearchQuery({ ...EMPTY_STORY_FILTERS, text: term }, "newest", 0, LIMIT);

    sendJson<StoryPage>(`/api/workspaces/${workspaceId}/stories?${query}`, "GET").then((response) => {
      if (current && response.ok && response.data) {
        setLoaded({ term, items: response.data.items });
        setActive(-1);
      }
    });

    return () => {
      current = false;
    };
  }, [enabled, term, workspaceId]);

  const visible = enabled && open && typed.length >= MIN_LENGTH && loaded.term === typed;
  const items = visible ? loaded.items : [];

  function pick(story: Story) {
    setOpen(false);
    setActive(-1);
    onPick?.(story);
  }

  function onKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (!visible) {
      if (event.key === "ArrowDown") setOpen(true);
      return;
    }

    if (event.key === "ArrowDown" && items.length > 0) {
      event.preventDefault();
      setActive((index) => (index + 1) % items.length);
    } else if (event.key === "ArrowUp" && items.length > 0) {
      event.preventDefault();
      setActive((index) => (index <= 0 ? items.length - 1 : index - 1));
    } else if (event.key === "Enter") {
      if (active >= 0 && items[active]) {
        event.preventDefault();
        pick(items[active]);
      } else {
        setOpen(false);
      }
    } else if (event.key === "Escape") {
      event.preventDefault();
      setOpen(false);
    }
  }

  const inputProps = enabled
    ? {
        role: "combobox" as const,
        "aria-autocomplete": "list" as const,
        "aria-expanded": visible,
        "aria-controls": id,
        "aria-activedescendant": visible && active >= 0 ? `${id}-${active}` : undefined,
        onFocus: () => setOpen(true),
        onBlur: () => setOpen(false),
        onKeyDown,
      }
    : {};

  return {
    inputProps,
    reopen: () => setOpen(true),
    list: { id, visible, items, active, onPick: pick, onHover: setActive },
  };
}

type StorySuggestionsProps = ReturnType<typeof useStorySuggestions>["list"];

export function StorySuggestions({ id, visible, items, active, onPick, onHover }: StorySuggestionsProps) {
  const t = useT();

  if (!visible) return null;

  return (
    <ul id={id} role="listbox" aria-label={t("filters.suggestions")} className={styles.list}>
      {items.length === 0 ? (
        <li className={styles.empty}>{t("filters.noSuggestions")}</li>
      ) : (
        items.map((story, index) => (
          <li
            key={story.id}
            id={`${id}-${index}`}
            role="option"
            aria-selected={index === active}
            className={`${styles.option} ${index === active ? styles.active : ""}`}
            onMouseDown={(event) => event.preventDefault()}
            onMouseEnter={() => onHover(index)}
            onClick={() => onPick(story)}
          >
            <PriorityBadge priority={story.priority} label={t(`priority.${story.priority}`)} />
            <span className={styles.key}>{story.key}</span>
            <span className={styles.title}>{story.title}</span>
          </li>
        ))
      )}
    </ul>
  );
}
