"use client";

import { IterationCw } from "lucide-react";
import { useEffect, useId, useState, type KeyboardEvent } from "react";
import { PriorityBadge } from "@/app/(app)/workspaces/[id]/board/PriorityBadge";
import { useT } from "@/i18n/client";
import type { Sprint, SprintPage, Story } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { EMPTY_STORY_FILTERS, storySearchQuery, type StoryPage } from "@/lib/story-filters";
import { useDebouncedValue } from "@/lib/use-debounced-value";
import styles from "./story-suggestions.module.css";

const MIN_LENGTH = 2;
const LIMIT = 6;
const SPRINT_LIMIT = 3;
const DELAY = 200;

export type Suggestion = { kind: "story"; story: Story } | { kind: "sprint"; sprint: Sprint };

const keyOf = (item: Suggestion) => (item.kind === "story" ? item.story.id : item.sprint.id);

export function useStorySuggestions(
  workspaceId: string | undefined,
  value: string,
  onPick: ((story: Story) => void) | undefined,
  onPickSprint?: (sprint: Sprint) => void,
) {
  const id = useId();
  const typed = value.trim();
  const term = useDebouncedValue(typed, DELAY);
  const enabled = Boolean(workspaceId && onPick);
  const withSprints = Boolean(onPickSprint);
  const [loaded, setLoaded] = useState<{ term: string; items: Suggestion[] }>({ term: "", items: [] });
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);

  useEffect(() => {
    if (!enabled || term.length < MIN_LENGTH) return;

    let current = true;
    const base = `/api/workspaces/${workspaceId}`;
    const query = storySearchQuery({ ...EMPTY_STORY_FILTERS, text: term }, "newest", 0, LIMIT);
    const sprintQuery = new URLSearchParams({ q: term, size: String(SPRINT_LIMIT) }).toString();

    Promise.all([
      withSprints ? sendJson<SprintPage>(`${base}/sprints?${sprintQuery}`, "GET") : Promise.resolve(null),
      sendJson<StoryPage>(`${base}/stories?${query}`, "GET"),
    ]).then(([sprints, stories]) => {
      if (!current || !stories.ok || !stories.data) return;

      setLoaded({
        term,
        items: [
          ...(sprints?.ok && sprints.data ? sprints.data.items.map((sprint) => ({ kind: "sprint" as const, sprint })) : []),
          ...stories.data.items.map((story) => ({ kind: "story" as const, story })),
        ],
      });
      setActive(-1);
    });

    return () => {
      current = false;
    };
  }, [enabled, withSprints, term, workspaceId]);

  const visible = enabled && open && typed.length >= MIN_LENGTH && loaded.term === typed;
  const items = visible ? loaded.items : [];

  function pick(item: Suggestion) {
    setOpen(false);
    setActive(-1);

    if (item.kind === "story") onPick?.(item.story);
    else onPickSprint?.(item.sprint);
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

  const firstStory = items.findIndex((item) => item.kind === "story");
  const hasSprints = items.some((item) => item.kind === "sprint");

  return (
    <ul id={id} role="listbox" aria-label={t("filters.suggestions")} className={styles.list}>
      {items.length === 0 ? (
        <li className={styles.empty}>{t("filters.noSuggestions")}</li>
      ) : (
        items.map((item, index) => (
          <li
            key={keyOf(item)}
            id={`${id}-${index}`}
            role="option"
            aria-selected={index === active}
            className={`${styles.option} ${index === active ? styles.active : ""} ${
              hasSprints && index === firstStory ? styles.divided : ""
            }`}
            onMouseDown={(event) => event.preventDefault()}
            onMouseEnter={() => onHover(index)}
            onClick={() => onPick(item)}
          >
            {item.kind === "story" ? (
              <>
                <PriorityBadge priority={item.story.priority} label={t(`priority.${item.story.priority}`)} />
                <span className={styles.key}>{item.story.key}</span>
                <span className={styles.title}>{item.story.title}</span>
              </>
            ) : (
              <>
                <IterationCw size={14} aria-hidden="true" className={styles.sprintIcon} />
                <span className={styles.title}>{item.sprint.name}</span>
                <span className={styles.kind}>{t(`sprints.states.${item.sprint.state}`)}</span>
              </>
            )}
          </li>
        ))
      )}
    </ul>
  );
}
