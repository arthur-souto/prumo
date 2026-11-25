import type { Priority, Story } from "./board-types";

export type StoryFilters = {
  text: string;
  priorities: Priority[];
  labels: string[];
  columnIds: string[];
  assigneeIds: string[];
  unassigned: boolean;
};

export const EMPTY_STORY_FILTERS: StoryFilters = {
  text: "",
  priorities: [],
  labels: [],
  columnIds: [],
  assigneeIds: [],
  unassigned: false,
};

export type StoryOrder = "newest" | "oldest" | "updated" | "priority";

export const STORY_ORDERS: StoryOrder[] = ["newest", "oldest", "updated", "priority"];

export const DEFAULT_STORY_ORDER: StoryOrder = "newest";

const ORDER_PARAMS: Record<StoryOrder, { sort: string; direction: string }> = {
  newest: { sort: "NUMBER", direction: "DESC" },
  oldest: { sort: "NUMBER", direction: "ASC" },
  updated: { sort: "UPDATED_AT", direction: "DESC" },
  priority: { sort: "PRIORITY", direction: "DESC" },
};

export type StoryPage = {
  items: Story[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
};

export function hasStoryFilters(filters: StoryFilters) {
  return (
    filters.text.trim() !== "" ||
    filters.priorities.length > 0 ||
    filters.labels.length > 0 ||
    filters.columnIds.length > 0 ||
    filters.assigneeIds.length > 0 ||
    filters.unassigned
  );
}

export function storySearchQuery(filters: StoryFilters, order: StoryOrder, page: number, size: number) {
  const query = new URLSearchParams();
  const text = filters.text.trim();

  if (text) query.set("q", text);
  filters.labels.forEach((label) => query.append("label", label));
  filters.priorities.forEach((priority) => query.append("priority", priority));
  filters.columnIds.forEach((columnId) => query.append("columnId", columnId));
  filters.assigneeIds.forEach((assigneeId) => query.append("assigneeId", assigneeId));
  if (filters.unassigned) query.set("unassigned", "true");
  query.set("sort", ORDER_PARAMS[order].sort);
  query.set("direction", ORDER_PARAMS[order].direction);
  query.set("page", String(page));
  query.set("size", String(size));

  return query.toString();
}

export function toggle<T>(values: T[], value: T) {
  return values.includes(value) ? values.filter((candidate) => candidate !== value) : [...values, value];
}
