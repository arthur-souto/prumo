import { screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { mockFetch, requests } from "@/test/fetch";
import { repository, subtask } from "@/test/fixtures";
import { router } from "@/test/navigation";
import { renderWithProviders } from "@/test/render";
import type { Subtask } from "@/lib/board-types";
import { SubtaskView } from "./SubtaskView";

const BASE = "/api/workspaces/w1/subtasks/PRU-11";
const item = subtask({ key: "PRU-11", storyKey: "PRU-1", title: "Write the tests" });

/** What the page loads on its own. */
const sections = [
  { method: "GET", path: `${BASE}/comments`, body: [] },
  { method: "GET", path: `${BASE}/activity`, body: [] },
];

function render(initial: Subtask = item, canBranch = true) {
  return renderWithProviders(
    <SubtaskView workspaceId="w1" initial={initial} storyTitle="Reset password" repository={repository()} canBranch={canBranch} />,
  );
}

describe("SubtaskView", () => {
  it("links back to the story it belongs to", () => {
    mockFetch(sections);

    render();

    expect(screen.getByRole("link", { name: "Back to PRU-1" })).toHaveAttribute("href", "/workspaces/w1/board");
    expect(screen.getByText(/Subtask of PRU-1/)).toHaveTextContent("Reset password");
  });

  it("saves the title and description", async () => {
    const fetch = mockFetch([...sections, { method: "PUT", path: BASE, body: { ...item, title: "Write more tests", description: "Front" } }]);
    const { user } = render();

    await user.clear(screen.getByRole("textbox", { name: "Title" }));
    await user.type(screen.getByRole("textbox", { name: "Title" }), "Write more tests");
    await user.type(screen.getByRole("textbox", { name: "Description" }), "Front");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("Changes saved")).toBeInTheDocument();
    expect(requests(fetch, "PUT")[0].body).toEqual({ title: "Write more tests", description: "Front" });
    expect(screen.getByRole("button", { name: "Save changes" })).toBeDisabled();
  });

  it("changes the status right away", async () => {
    const fetch = mockFetch([...sections, { method: "PUT", path: `${BASE}/status`, body: { ...item, status: "IN_REVIEW" } }]);
    const { user } = render();

    await user.selectOptions(screen.getByRole("combobox", { name: "Status" }), "In review");

    await waitFor(() => expect(screen.getByRole("combobox", { name: "Status" })).toHaveValue("IN_REVIEW"));
    expect(requests(fetch, "PUT")[0].body).toEqual({ status: "IN_REVIEW" });
  });

  it("creates the branch with the suggested name when none is typed", async () => {
    const fetch = mockFetch([
      ...sections,
      { method: "GET", path: `${BASE}/branch-suggestion`, body: { name: "feature/PRU-11-write-the-tests" } },
      { method: "POST", path: `${BASE}/branch`, body: { ...item, branchName: "feature/PRU-11-write-the-tests" } },
    ]);
    const { user } = render();

    await user.click(screen.getByRole("button", { name: "Create branch" }));

    expect(await screen.findByRole("link", { name: "feature/PRU-11-write-the-tests" })).toHaveAttribute(
      "href",
      "https://github.com/arthur/prumo/tree/feature/PRU-11-write-the-tests",
    );
    expect(requests(fetch, "POST")[0].body).toEqual({ name: "feature/PRU-11-write-the-tests" });
  });

  it("uses the typed branch name and shows GitHub's refusal", async () => {
    const fetch = mockFetch([...sections, { method: "POST", path: `${BASE}/branch`, status: 409, body: { code: "conflict", reason: "BRANCH_EXISTS" } }]);
    const { user } = render();

    await user.type(screen.getByRole("textbox", { name: "Branch name" }), " feature/mine ");
    await user.click(screen.getByRole("button", { name: "Create branch" }));

    expect(await screen.findByText("A branch with this name already exists.")).toBeInTheDocument();
    expect(requests(fetch, "GET", `${BASE}/branch-suggestion`)).toHaveLength(0);
    expect(requests(fetch, "POST")[0].body).toEqual({ name: "feature/mine" });
  });

  it("offers no branch without a repository, and shows the branch once there is one", () => {
    mockFetch(sections);
    const first = render(item, false);
    expect(screen.getByText("Link a repository to this story to create a branch.")).toBeInTheDocument();
    first.unmount();

    render({ ...item, branchName: "feature/PRU-11-x" });
    expect(screen.queryByRole("button", { name: "Create branch" })).not.toBeInTheDocument();
    expect(screen.getByText("feature/PRU-11-x")).toBeInTheDocument();
  });

  it("goes back to the board after deleting", async () => {
    mockFetch([...sections, { method: "DELETE", path: BASE, status: 204 }]);
    const { user } = render();

    await user.click(screen.getByRole("button", { name: "Delete subtask" }));
    await user.click(within(screen.getByRole("dialog", { name: "Delete PRU-11?" })).getByRole("button", { name: "Delete subtask" }));

    await waitFor(() => expect(router.push).toHaveBeenCalledWith("/workspaces/w1/board"));
    expect(router.refresh).toHaveBeenCalled();
  });
});
