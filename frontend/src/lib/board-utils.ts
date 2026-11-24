import type { Board, Priority, Story } from "./board-types";

export type Layout = Record<string, string[]>;

export type Filters = { text: string; priority: Priority | ""; label: string };

export const EMPTY_FILTERS: Filters = { text: "", priority: "", label: "" };

export function buildLayout(board: Board): Layout {
  const layout: Layout = {};

  for (const column of board.columns) {
    layout[column.id] = [];
  }

  for (const story of board.stories) {
    (layout[story.columnId] ??= []).push(story.id);
  }

  return layout;
}

export function hasFilters(filters: Filters) {
  return filters.text.trim() !== "" || filters.priority !== "" || filters.label !== "";
}

export function matchesFilters(story: Story, filters: Filters) {
  const text = filters.text.trim().toLowerCase();

  if (filters.priority && story.priority !== filters.priority) {
    return false;
  }

  if (filters.label && !story.labels.some((label) => label.toLowerCase() === filters.label.toLowerCase())) {
    return false;
  }

  if (text === "") {
    return true;
  }

  return (
    story.title.toLowerCase().includes(text) ||
    story.key.toLowerCase().includes(text) ||
    (story.description ?? "").toLowerCase().includes(text)
  );
}

export function neighbours(items: string[], id: string) {
  const index = items.indexOf(id);

  return {
    after: index > 0 ? items[index - 1] : undefined,
    before: index >= 0 && index < items.length - 1 ? items[index + 1] : undefined,
  };
}

export function allLabels(stories: Story[]) {
  const unique = new Map<string, string>();

  for (const story of stories) {
    for (const label of story.labels) {
      unique.set(label.toLowerCase(), unique.get(label.toLowerCase()) ?? label);
    }
  }

  return [...unique.values()].sort((a, b) => a.localeCompare(b));
}

export function parseLabels(text: string) {
  return text
    .split(",")
    .map((label) => label.trim())
    .filter((label) => label !== "");
}

export function isHttpUrl(value: string) {
  return value.startsWith("https://") || value.startsWith("http://");
}
