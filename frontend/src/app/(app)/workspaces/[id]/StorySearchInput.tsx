"use client";

import { Search } from "lucide-react";
import { StorySuggestions, useStorySuggestions } from "@/components/search/StorySuggestions";
import { useT } from "@/i18n/client";
import type { Story } from "@/lib/board-types";
import styles from "./story-filter-bar.module.css";

type StorySearchInputProps = {
  value: string;
  onChange: (value: string) => void;
  workspaceId?: string;
  onPick?: (story: Story) => void;
};

export function StorySearchInput({ value, onChange, workspaceId, onPick }: StorySearchInputProps) {
  const t = useT();
  const suggestions = useStorySuggestions(workspaceId, value, onPick);

  return (
    <span className={styles.searchWrap}>
      <Search size={14} aria-hidden="true" className={styles.searchIcon} />
      <input
        className={styles.search}
        type="search"
        value={value}
        placeholder={t("board.searchPlaceholder")}
        aria-label={t("board.searchLabel")}
        {...suggestions.inputProps}
        onChange={(event) => {
          onChange(event.target.value);
          suggestions.reopen();
        }}
      />
      <StorySuggestions {...suggestions.list} />
    </span>
  );
}
