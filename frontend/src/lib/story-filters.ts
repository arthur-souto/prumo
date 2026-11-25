import { PRIORITIES, type Priority, type Story } from "./board-types";

export type StoryFilters = {
  text: string;
  priorities: Priority[];
  labels: string[];
  columnIds: string[];
  assigneeIds: string[];
  unassigned: boolean;
  sprintIds: string[];
  noSprint: boolean;
};

export const EMPTY_STORY_FILTERS: StoryFilters = {
  text: "",
  priorities: [],
  labels: [],
  columnIds: [],
  assigneeIds: [],
  unassigned: false,
  sprintIds: [],
  noSprint: false,
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
    filters.unassigned ||
    filters.sprintIds.length > 0 ||
    filters.noSprint
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
  filters.sprintIds.forEach((sprintId) => query.append("sprintId", sprintId));
  if (filters.noSprint) query.set("noSprint", "true");
  query.set("sort", ORDER_PARAMS[order].sort);
  query.set("direction", ORDER_PARAMS[order].direction);
  query.set("page", String(page));
  query.set("size", String(size));

  return query.toString();
}

export function toggle<T>(values: T[], value: T) {
  return values.includes(value) ? values.filter((candidate) => candidate !== value) : [...values, value];
}

export function normalizeText(value: string) {
  return value.normalize("NFD").replace(/\p{M}+/gu, "").toLowerCase();
}

export function matchesStoryFilters(story: Story, filters: StoryFilters) {
  if (filters.priorities.length > 0 && !filters.priorities.includes(story.priority)) {
    return false;
  }

  if (filters.columnIds.length > 0 && !filters.columnIds.includes(story.columnId)) {
    return false;
  }

  if (filters.labels.length > 0) {
    const wanted = new Set(filters.labels.map((label) => label.toLowerCase()));

    if (!story.labels.some((label) => wanted.has(label.toLowerCase()))) {
      return false;
    }
  }

  if (filters.assigneeIds.length > 0 || filters.unassigned) {
    const assigned = story.assigneeId !== null && filters.assigneeIds.includes(story.assigneeId);
    const unassigned = filters.unassigned && story.assigneeId === null;

    if (!assigned && !unassigned) {
      return false;
    }
  }

  if (filters.sprintIds.length > 0 || filters.noSprint) {
    const planned = story.sprintId !== null && filters.sprintIds.includes(story.sprintId);
    const backlog = filters.noSprint && story.sprintId === null;

    if (!planned && !backlog) {
      return false;
    }
  }

  const text = normalizeText(filters.text.trim());

  if (text === "") {
    return true;
  }

  return (
    normalizeText(story.title).includes(text) ||
    story.key.toLowerCase().includes(text) ||
    normalizeText(story.description ?? "").includes(text)
  );
}

const NO_ASSIGNEE = "none";
const NO_SPRINT = "none";

export function readStoryFilters(params: URLSearchParams): StoryFilters {
  const assignees = params.getAll("assignee");
  const sprints = params.getAll("sprint");

  return {
    text: params.get("q") ?? "",
    priorities: params.getAll("priority").filter((value): value is Priority => (PRIORITIES as string[]).includes(value)),
    labels: params.getAll("label").filter((value) => value.trim() !== ""),
    columnIds: params.getAll("column").filter((value) => value !== ""),
    assigneeIds: assignees.filter((value) => value !== "" && value !== NO_ASSIGNEE),
    unassigned: assignees.includes(NO_ASSIGNEE),
    sprintIds: sprints.filter((value) => value !== "" && value !== NO_SPRINT),
    noSprint: sprints.includes(NO_SPRINT),
  };
}

export function writeStoryFilters(params: URLSearchParams, filters: StoryFilters) {
  for (const key of ["q", "priority", "label", "column", "assignee", "sprint", "page"]) {
    params.delete(key);
  }

  const text = filters.text.trim();

  if (text) params.set("q", text);
  filters.priorities.forEach((priority) => params.append("priority", priority));
  filters.labels.forEach((label) => params.append("label", label));
  filters.columnIds.forEach((columnId) => params.append("column", columnId));
  filters.assigneeIds.forEach((assigneeId) => params.append("assignee", assigneeId));
  if (filters.unassigned) params.append("assignee", NO_ASSIGNEE);
  filters.sprintIds.forEach((sprintId) => params.append("sprint", sprintId));
  if (filters.noSprint) params.append("sprint", NO_SPRINT);
}

export function readStoryOrder(params: URLSearchParams): StoryOrder {
  const value = params.get("sort");
  return STORY_ORDERS.includes(value as StoryOrder) ? (value as StoryOrder) : DEFAULT_STORY_ORDER;
}

export function readPage(params: URLSearchParams) {
  const value = Number(params.get("page"));
  return Number.isInteger(value) && value > 1 ? value - 1 : 0;
}
