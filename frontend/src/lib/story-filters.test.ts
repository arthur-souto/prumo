import { describe, expect, it } from "vitest";
import { story } from "@/test/fixtures";
import {
  EMPTY_STORY_FILTERS,
  matchesStoryFilters,
  readPage,
  readStoryFilters,
  readStoryOrder,
  storySearchQuery,
  writeStoryFilters,
  type StoryFilters,
} from "./story-filters";

const filters = (overrides: Partial<StoryFilters>): StoryFilters => ({ ...EMPTY_STORY_FILTERS, ...overrides });

describe("filters in the URL", () => {
  it("survive a round trip, including the 'no assignee' and 'backlog' choices", () => {
    const written = filters({
      text: " login ",
      priorities: ["HIGH"],
      labels: ["api"],
      columnIds: ["todo"],
      assigneeIds: ["m1"],
      unassigned: true,
      sprintIds: ["s1"],
      noSprint: true,
    });
    const params = new URLSearchParams("sort=oldest&page=3");

    writeStoryFilters(params, written);

    expect(readStoryFilters(params)).toEqual({ ...written, text: "login" });
    expect(params.get("sort")).toBe("oldest");
    // a new filter starts again from the first page
    expect(params.has("page")).toBe(false);
  });

  it("ignore values that do not exist", () => {
    expect(readStoryFilters(new URLSearchParams("priority=SOON&label=&column="))).toEqual(EMPTY_STORY_FILTERS);
    expect(readStoryOrder(new URLSearchParams("sort=random"))).toBe("newest");
    expect(readPage(new URLSearchParams("page=0"))).toBe(0);
    expect(readPage(new URLSearchParams("page=abc"))).toBe(0);
    expect(readPage(new URLSearchParams("page=3"))).toBe(2);
  });
});

describe("storySearchQuery", () => {
  it("speaks the API's parameter names and maps the order to sort and direction", () => {
    const query = new URLSearchParams(
      storySearchQuery(filters({ text: "x", columnIds: ["todo"], assigneeIds: ["m1"], unassigned: true, noSprint: true }), "priority", 2, 25),
    );

    expect(Object.fromEntries(query)).toEqual({
      q: "x",
      columnId: "todo",
      assigneeId: "m1",
      unassigned: "true",
      noSprint: "true",
      sort: "PRIORITY",
      direction: "DESC",
      page: "2",
      size: "25",
    });
  });
});

describe("matchesStoryFilters", () => {
  const login = story({ key: "PRU-9", title: "Redefinição de senha", labels: ["API"], assigneeId: null, sprintId: "s1", priority: "HIGH" });

  it("finds by title ignoring accents and case, by key and by description", () => {
    expect(matchesStoryFilters(login, filters({ text: "redefinicao" }))).toBe(true);
    expect(matchesStoryFilters(login, filters({ text: "pru-9" }))).toBe(true);
    expect(matchesStoryFilters(story({ description: "Login flow" }), filters({ text: "login" }))).toBe(true);
    expect(matchesStoryFilters(login, filters({ text: "board" }))).toBe(false);
  });

  it("combines the filters with AND, and the options inside one filter with OR", () => {
    expect(matchesStoryFilters(login, filters({ labels: ["api"], priorities: ["HIGH", "LOW"] }))).toBe(true);
    expect(matchesStoryFilters(login, filters({ labels: ["api"], priorities: ["LOW"] }))).toBe(false);
  });

  it("treats 'unassigned' and 'backlog' as options next to the people and sprints", () => {
    expect(matchesStoryFilters(login, filters({ assigneeIds: ["m1"], unassigned: true }))).toBe(true);
    expect(matchesStoryFilters(login, filters({ assigneeIds: ["m1"] }))).toBe(false);
    expect(matchesStoryFilters(login, filters({ noSprint: true }))).toBe(false);
    expect(matchesStoryFilters(story({ sprintId: null }), filters({ sprintIds: ["s1"], noSprint: true }))).toBe(true);
  });
});
