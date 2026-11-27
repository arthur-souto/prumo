import { describe, expect, it } from "vitest";
import { board, story } from "@/test/fixtures";
import { allLabels, buildLayout, neighbours, parseLabels } from "./board-utils";

describe("buildLayout", () => {
  it("puts each story under its column, keeping the order, and lists empty columns too", () => {
    const first = story({ columnId: "todo" });
    const second = story({ columnId: "done" });
    const third = story({ columnId: "todo" });

    expect(buildLayout(board({ stories: [first, second, third] }))).toEqual({
      todo: [first.id, third.id],
      doing: [],
      review: [],
      done: [second.id],
    });
  });
});

describe("neighbours", () => {
  it("names the cards around a position, so a move can be ranked between them", () => {
    expect(neighbours(["a", "b", "c"], "b")).toEqual({ after: "a", before: "c" });
    expect(neighbours(["a", "b", "c"], "a")).toEqual({ after: undefined, before: "b" });
    expect(neighbours(["a", "b", "c"], "c")).toEqual({ after: "b", before: undefined });
    expect(neighbours(["a"], "missing")).toEqual({ after: undefined, before: undefined });
  });
});

describe("labels", () => {
  it("lists each label once ignoring case, keeping the first spelling, in alphabetical order", () => {
    const stories = [story({ labels: ["api", "Front"] }), story({ labels: ["API", "back"] })];

    expect(allLabels(stories)).toEqual(["api", "back", "Front"]);
  });

  it("parses a comma separated field without blanks", () => {
    expect(parseLabels(" api, ,front ,")).toEqual(["api", "front"]);
    expect(parseLabels("")).toEqual([]);
  });
});
