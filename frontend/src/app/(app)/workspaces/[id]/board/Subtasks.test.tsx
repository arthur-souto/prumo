import { screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { mockFetch, requests } from "@/test/fetch";
import { repository, story, subtask } from "@/test/fixtures";
import { renderWithProviders } from "@/test/render";
import { Subtasks } from "./Subtasks";

const BASE = "/api/workspaces/w1";
const parent = story({ key: "PRU-1", repositoryId: "repo-1" });

function render(storyOverrides = {}) {
  const onProgress = vi.fn();
  const view = renderWithProviders(
    <Subtasks workspaceId="w1" story={{ ...parent, ...storyOverrides }} repository={repository()} onProgress={onProgress} />,
  );

  return { ...view, onProgress };
}

function branchRoutes(results: Record<string, { status: number; body?: unknown }>) {
  return Object.entries(results).flatMap(([key, result]) => [
    { method: "GET", path: `${BASE}/subtasks/${key}/branch-suggestion`, body: { name: `feature/${key}-x` } },
    { method: "POST", path: `${BASE}/subtasks/${key}/branch`, ...result },
  ]);
}

describe("Subtasks", () => {
  it("lists the subtasks with their progress", async () => {
    mockFetch([{ method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [subtask({ status: "DONE" }), subtask()] }]);

    render();

    expect(await screen.findByText("1/2")).toBeInTheDocument();
    expect(screen.getByRole("progressbar", { name: "Subtasks" })).toHaveAttribute("aria-valuenow", "1");
  });

  it("adds a subtask with Enter and reports the new progress", async () => {
    const created = subtask({ key: "PRU-30", title: "Write the tests" });
    const fetch = mockFetch([
      { method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [] },
      { method: "POST", path: `${BASE}/stories/PRU-1/subtasks`, status: 201, body: created },
    ]);
    const { user, onProgress } = render();
    await screen.findByText("No subtasks yet.");

    await user.type(screen.getByRole("textbox", { name: "Add subtask" }), "  Write the tests {Enter}");

    expect(await screen.findByRole("link", { name: "Write the tests" })).toBeInTheDocument();
    expect(requests(fetch, "POST")[0].body).toEqual({ title: "Write the tests" });
    expect(onProgress).toHaveBeenLastCalledWith({ total: 1, done: 0 });
    expect(await screen.findByText("PRU-30 created")).toBeInTheDocument();
  });

  it("creates the branch with the suggested name when asked while adding", async () => {
    const created = subtask({ key: "PRU-30" });
    const fetch = mockFetch([
      { method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [] },
      { method: "POST", path: `${BASE}/stories/PRU-1/subtasks`, status: 201, body: created },
      ...branchRoutes({ "PRU-30": { status: 200, body: { ...created, branchName: "feature/PRU-30-x" } } }),
    ]);
    const { user } = render();
    await screen.findByText("No subtasks yet.");

    await user.click(screen.getByRole("checkbox", { name: "Create branch" }));
    await user.type(screen.getByRole("textbox", { name: "Add subtask" }), "Tests{Enter}");

    expect(await screen.findByText("feature/PRU-30-x")).toBeInTheDocument();
    expect(requests(fetch, "POST", `${BASE}/subtasks/PRU-30/branch`)[0].body).toEqual({ name: "feature/PRU-30-x" });
  });

  it("does not offer branches while the story has no repository", async () => {
    mockFetch([{ method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [subtask()] }]);

    render({ repositoryId: null });

    expect(await screen.findByText("Link a repository to this story to create a branch.")).toBeInTheDocument();
    expect(screen.getByRole("checkbox", { name: "Create branch" })).toBeDisabled();
    expect(screen.queryByRole("checkbox", { name: /^Select / })).not.toBeInTheDocument();
  });

  describe("branches for several subtasks", () => {
    const withBranch = subtask({ key: "PRU-10", branchName: "feature/PRU-10-done" });
    const first = subtask({ key: "PRU-11", title: "First" });
    const second = subtask({ key: "PRU-12", title: "Second" });

    it("only subtasks without a branch can be selected, and 'select all' picks them all", async () => {
      mockFetch([{ method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [withBranch, first, second] }]);
      const { user } = render();

      await user.click(await screen.findByRole("checkbox", { name: "Select subtasks without a branch" }));

      expect(screen.queryByRole("checkbox", { name: "Select PRU-10" })).not.toBeInTheDocument();
      expect(screen.getByRole("checkbox", { name: "Select PRU-11" })).toBeChecked();
      expect(screen.getByRole("checkbox", { name: "Select PRU-12" })).toBeChecked();
      expect(screen.getByRole("button", { name: "Create 2 branches" })).toBeInTheDocument();
    });

    it("creates one branch after the other with the suggested names", async () => {
      const fetch = mockFetch([
        { method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [first, second] },
        ...branchRoutes({
          "PRU-11": { status: 200, body: { ...first, branchName: "feature/PRU-11-x" } },
          "PRU-12": { status: 200, body: { ...second, branchName: "feature/PRU-12-x" } },
        }),
      ]);
      const { user } = render();

      await user.click(await screen.findByRole("checkbox", { name: "Select PRU-11" }));
      await user.click(screen.getByRole("checkbox", { name: "Select PRU-12" }));
      await user.click(screen.getByRole("button", { name: "Create 2 branches" }));

      expect(await screen.findByText("2 branches created")).toBeInTheDocument();
      expect(screen.getByText("feature/PRU-11-x")).toBeInTheDocument();
      expect(screen.getByText("feature/PRU-12-x")).toBeInTheDocument();
      expect(requests(fetch).map((call) => `${call.method} ${call.url.pathname}`).slice(1)).toEqual([
        `GET ${BASE}/subtasks/PRU-11/branch-suggestion`,
        `POST ${BASE}/subtasks/PRU-11/branch`,
        `GET ${BASE}/subtasks/PRU-12/branch-suggestion`,
        `POST ${BASE}/subtasks/PRU-12/branch`,
      ]);
      // nothing left without a branch, so the selection bar goes away
      expect(screen.queryByRole("checkbox", { name: "Select subtasks without a branch" })).not.toBeInTheDocument();
    });

    it("keeps the failed ones selected and says why each one failed", async () => {
      mockFetch([
        { method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [first, second] },
        ...branchRoutes({
          "PRU-11": { status: 200, body: { ...first, branchName: "feature/PRU-11-x" } },
          "PRU-12": { status: 409, body: { code: "conflict", reason: "BRANCH_EXISTS" } },
        }),
      ]);
      const { user } = render();

      await user.click(await screen.findByRole("checkbox", { name: "Select subtasks without a branch" }));
      await user.click(screen.getByRole("button", { name: "Create 2 branches" }));

      expect(await screen.findByText("1 branch created")).toBeInTheDocument();
      expect(screen.getByRole("alert")).toHaveTextContent("PRU-12: A branch with this name already exists.");
      expect(screen.getByRole("checkbox", { name: "Select PRU-12" })).toBeChecked();
      expect(screen.getByRole("button", { name: "Create 1 branch" })).toBeInTheDocument();
    });
  });

  it("changes the status and asks before deleting", async () => {
    const item = subtask({ key: "PRU-11", title: "First" });
    const fetch = mockFetch([
      { method: "GET", path: `${BASE}/stories/PRU-1/subtasks`, body: [item] },
      { method: "PUT", path: `${BASE}/subtasks/PRU-11/status`, body: { ...item, status: "DONE" } },
      { method: "DELETE", path: `${BASE}/subtasks/PRU-11`, status: 204 },
    ]);
    const { user, onProgress } = render();

    await user.selectOptions(await screen.findByRole("combobox", { name: "Subtask status" }), "DONE");
    await waitFor(() => expect(onProgress).toHaveBeenLastCalledWith({ total: 1, done: 1 }));
    expect(requests(fetch, "PUT")[0].body).toEqual({ status: "DONE" });

    await user.click(screen.getByRole("button", { name: "Delete subtask" }));
    await user.click(within(screen.getByRole("dialog", { name: "Delete PRU-11?" })).getByRole("button", { name: "Delete subtask" }));

    await waitFor(() => expect(onProgress).toHaveBeenLastCalledWith({ total: 0, done: 0 }));
    expect(screen.queryByText("First")).not.toBeInTheDocument();
  });
});
