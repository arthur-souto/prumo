import { screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { mockFetch, requests } from "@/test/fetch";
import { board, sprint, story, storyPage } from "@/test/fixtures";
import { router } from "@/test/navigation";
import { renderWithProviders } from "@/test/render";
import type { Story } from "@/lib/board-types";
import { BacklogView } from "./BacklogView";

const BASE = "/api/workspaces/w1";
const STORIES = `${BASE}/stories`;
const planned = sprint({ name: "Sprint 7" });

function render() {
  return renderWithProviders(
    <BacklogView workspaceId="w1" board={board()} repositories={[]} members={[]} sprints={[planned]} hasGithubToken={false} />,
  );
}

function updated(original: Story, changes: Partial<Story>) {
  return { method: "PUT", path: `${STORIES}/${original.key}`, body: { ...original, ...changes } };
}

describe("BacklogView", () => {
  it("asks the API for the page, order and filters in the URL", async () => {
    router.at("/workspaces/w1/backlog?q=login&priority=HIGH&sort=oldest&page=2");
    const fetch = mockFetch([{ method: "GET", path: STORIES, body: storyPage([story({ title: "Login" })], { page: 1, totalItems: 26 }) }]);

    render();

    expect(await screen.findByRole("button", { name: "Login" })).toBeInTheDocument();
    expect(screen.getByRole("status")).toHaveTextContent("26–26 of 26");
    // the search box also asks for suggestions (size 6); the table is the page of 25
    const table = requests(fetch, "GET", STORIES).find((call) => call.url.searchParams.get("size") === "25");
    expect(Object.fromEntries(table!.url.searchParams)).toMatchObject({
      q: "login",
      priority: "HIGH",
      sort: "NUMBER",
      direction: "ASC",
      page: "1",
      size: "25",
    });
  });

  it("pages through the results by changing the URL", async () => {
    const fetch = mockFetch([
      { method: "GET", path: STORIES, when: (url) => url.searchParams.get("page") === "0", body: storyPage([story({ title: "First page" })], { totalItems: 30 }) },
      { method: "GET", path: STORIES, when: (url) => url.searchParams.get("page") === "1", body: storyPage([story({ title: "Second page" })], { page: 1, totalItems: 30 }) },
    ]);
    const { user } = render();
    await screen.findByText("Page 1 of 2");
    expect(screen.getByRole("button", { name: "Previous page" })).toBeDisabled();

    await user.click(screen.getByRole("button", { name: "Next page" }));

    expect(await screen.findByRole("button", { name: "Second page" })).toBeInTheDocument();
    expect(router.params.get("page")).toBe("2");
    expect(requests(fetch, "GET", STORIES).map((call) => call.url.searchParams.get("page"))).toEqual(["0", "1"]);
  });

  it("changing the priority in a row sends the whole story back, as the API replaces it", async () => {
    const row = story({ key: "PRU-4", title: "Row", priority: "LOW", labels: ["api"], storyPoints: 3 });
    const fetch = mockFetch([{ method: "GET", path: STORIES, body: storyPage([row]) }, updated(row, { priority: "URGENT" })]);
    const { user } = render();

    await user.selectOptions(await screen.findByRole("combobox", { name: "Priority: PRU-4" }), "URGENT");

    await waitFor(() => expect(screen.getByRole("combobox", { name: "Priority: PRU-4" })).toHaveValue("URGENT"));
    expect(requests(fetch, "PUT")[0].body).toMatchObject({ title: "Row", priority: "URGENT", labels: ["api"], storyPoints: 3 });
  });

  it("plans a row into a sprint and back to the backlog", async () => {
    const row = story({ key: "PRU-4" });
    const fetch = mockFetch([
      { method: "GET", path: STORIES, body: storyPage([row]) },
      { method: "PUT", path: `${STORIES}/PRU-4/sprint`, answers: [{ body: { ...row, sprintId: planned.id } }, { body: { ...row, sprintId: null } }] },
    ]);
    const { user } = render();
    const select = await screen.findByRole("combobox", { name: "Sprint: PRU-4" });

    await user.selectOptions(select, "Sprint 7");
    await waitFor(() => expect(select).toHaveValue(planned.id));
    await user.selectOptions(select, "Backlog");

    await waitFor(() => expect(select).toHaveValue(""));
    expect(requests(fetch, "PUT").map((call) => call.body)).toEqual([{ sprintId: planned.id }, { sprintId: null }]);
  });

  describe("bulk actions", () => {
    const a = story({ key: "PRU-1", title: "A" });
    const b = story({ key: "PRU-2", title: "B" });

    it("moves every selected story and says how many", async () => {
      const fetch = mockFetch([
        { method: "GET", path: STORIES, body: storyPage([a, b]) },
        { method: "PUT", path: `${STORIES}/PRU-1/move`, body: { ...a, columnId: "done" } },
        { method: "PUT", path: `${STORIES}/PRU-2/move`, body: { ...b, columnId: "done" } },
      ]);
      const { user } = render();

      await user.click(await screen.findByRole("checkbox", { name: "Select all stories" }));
      const bar = screen.getByRole("region", { name: "Bulk actions" });
      expect(bar).toHaveTextContent("2 selected");
      await user.selectOptions(within(bar).getByRole("combobox", { name: "Move to…" }), "Done");

      expect(await screen.findByText("2 stories updated")).toBeInTheDocument();
      expect(requests(fetch, "PUT").map((call) => call.body)).toEqual([{ columnId: "done" }, { columnId: "done" }]);
      expect(screen.getByRole("combobox", { name: "Status: PRU-1" })).toHaveValue("done");
    });

    it("says how many failed when only some of them could be changed", async () => {
      mockFetch([
        { method: "GET", path: STORIES, body: storyPage([a, b]) },
        updated(a, { priority: "HIGH" }),
        { method: "PUT", path: `${STORIES}/PRU-2`, status: 403, body: { code: "forbidden" } },
      ]);
      const { user } = render();

      await user.click(await screen.findByRole("checkbox", { name: "Select PRU-1" }));
      await user.click(screen.getByRole("checkbox", { name: "Select PRU-2" }));
      await user.selectOptions(screen.getByRole("combobox", { name: "Set priority…" }), "HIGH");

      expect(await screen.findByText("1 of 2 could not be updated")).toBeInTheDocument();
    });

    it("deletes the selected stories after a confirmation and reloads the page", async () => {
      const fetch = mockFetch([
        { method: "GET", path: STORIES, answers: [{ body: storyPage([a, b]) }, { body: storyPage([b]) }] },
        { method: "DELETE", path: `${STORIES}/PRU-1`, status: 204 },
      ]);
      const { user } = render();

      await user.click(await screen.findByRole("checkbox", { name: "Select PRU-1" }));
      await user.click(screen.getByRole("button", { name: "Delete" }));
      expect(requests(fetch, "DELETE")).toHaveLength(0);
      await user.click(within(screen.getByRole("dialog", { name: "Delete 1 story?" })).getByRole("button", { name: "Delete" }));

      expect(await screen.findByText("1 story deleted")).toBeInTheDocument();
      await waitFor(() => expect(screen.queryByRole("button", { name: "A" })).not.toBeInTheDocument());
      expect(requests(fetch, "GET", STORIES)).toHaveLength(2);
      expect(screen.queryByRole("region", { name: "Bulk actions" })).not.toBeInTheDocument();
    });
  });

  it("offers to try again when the search fails", async () => {
    mockFetch([{ method: "GET", path: STORIES, answers: [{ status: 503 }, { body: storyPage([story({ title: "Back" })]) }] }]);
    const { user } = render();

    await user.click(await screen.findByRole("button", { name: /try again/i }));

    expect(await screen.findByRole("button", { name: "Back" })).toBeInTheDocument();
  });

  it("invites to create the first story when the backlog is empty", async () => {
    mockFetch([{ method: "GET", path: STORIES, body: storyPage([]) }]);

    render();

    expect(await screen.findByText("No stories yet")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Create story" })).toHaveAttribute("href", "/workspaces/w1/board?create=1");
  });

  it("says nothing matched instead of the empty backlog while filtering", async () => {
    router.at("/workspaces/w1/backlog?q=zzz");
    mockFetch([{ method: "GET", path: STORIES, body: storyPage([]) }]);

    render();

    expect(await screen.findByText('No stories match "zzz"')).toBeInTheDocument();
  });
});
