import { fireEvent, screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { mockFetch, requests } from "@/test/fetch";
import { board, repository, story } from "@/test/fixtures";
import { router } from "@/test/navigation";
import { renderWithProviders } from "@/test/render";
import type { Board, Story } from "@/lib/board-types";
import { BoardView } from "./BoardView";

const BASE = "/api/workspaces/w1";
const login = story({ key: "PRU-1", title: "Login page", columnId: "todo" });
const reset = story({ key: "PRU-2", title: "Reset password", columnId: "todo" });
const deploy = story({ key: "PRU-3", title: "Deploy", columnId: "done" });

/** What the story drawer loads on its own when it opens. */
function drawerOf(key: string) {
  return ["subtasks", "comments", "activity"].map((section) => ({ method: "GET", path: `${BASE}/stories/${key}/${section}`, body: [] }));
}

function render({ initial = board({ stories: [login, reset, deploy] }), canManageBoard = true, repositories = [] as ReturnType<typeof repository>[] } = {}) {
  return renderWithProviders(
    <BoardView
      workspaceId="w1"
      canManageBoard={canManageBoard}
      initialBoard={initial}
      initialRepositories={repositories}
      members={[]}
      sprints={[]}
      hasGithubToken={false}
    />,
  );
}

// hidden: an open drawer is modal, so Radix hides the board from assistive tech while it is open
const column = (name: string) => screen.getByRole("region", { name, hidden: true });
// each column also has its own "Create story"; the toolbar one comes first
const createButton = () => screen.getAllByRole("button", { name: "Create story" })[0];
// Radix names the drawer after its title, which is the story key
const drawer = (key: string) => screen.findByRole("dialog", { name: key });

describe("BoardView", () => {
  it("shows each story in its column", () => {
    mockFetch([]);

    render();

    expect(within(column("To Do")).getByText("Login page")).toBeInTheDocument();
    expect(within(column("To Do")).getByText("Reset password")).toBeInTheDocument();
    expect(within(column("Done")).getByText("Deploy")).toBeInTheDocument();
    expect(within(column("In Review")).getByText("No stories")).toBeInTheDocument();
  });

  it("filters the cards by the text in the URL and warns that dragging is off", () => {
    router.at("/workspaces/w1/board?q=reset");
    mockFetch([{ method: "GET", path: `${BASE}/stories`, body: { items: [], page: 0, size: 6, totalItems: 0, totalPages: 0 } }]);

    render();

    expect(within(column("To Do")).queryByText("Login page")).not.toBeInTheDocument();
    expect(within(column("To Do")).getByText("Reset password")).toBeInTheDocument();
    expect(within(column("Done")).getByText("No match")).toBeInTheDocument();
    expect(screen.getByText("Filters are on. Clear them to drag cards.")).toBeInTheDocument();
  });

  it("creates a story, puts it at the end of its column and opens it", async () => {
    const created: Story = story({ key: "PRU-9", title: "New story", columnId: "todo", priority: "HIGH" });
    const fetch = mockFetch([{ method: "POST", path: `${BASE}/stories`, status: 201, body: created }, ...drawerOf("PRU-9")]);
    const { user } = render();

    await user.click(createButton());
    const dialog = screen.getByRole("dialog", { name: "Create story" });
    await user.type(within(dialog).getByRole("textbox", { name: "Title" }), "New story");
    await user.selectOptions(within(dialog).getByRole("combobox", { name: "Priority" }), "HIGH");
    await user.click(within(dialog).getByRole("button", { name: "Create" }));

    expect(await drawer("PRU-9")).toBeInTheDocument();
    // the first column is the server's choice, so no columnId is sent
    expect(requests(fetch, "POST")[0].body).toEqual({ title: "New story", priority: "HIGH" });
    expect(within(column("To Do")).getByText("New story")).toBeInTheDocument();
  });

  it("opens the create dialog with the c shortcut, but not while typing", async () => {
    mockFetch([{ method: "GET", path: `${BASE}/stories`, body: { items: [], page: 0, size: 6, totalItems: 0, totalPages: 0 } }]);
    const { user } = render();

    await user.type(screen.getByLabelText("Search stories"), "c");
    expect(screen.queryByRole("dialog", { name: "Create story" })).not.toBeInTheDocument();

    await user.click(document.body);
    await user.keyboard("c");
    expect(screen.getByRole("dialog", { name: "Create story" })).toBeInTheDocument();
  });

  it("opens the create dialog from a ?create=1 link and drops the parameter", async () => {
    router.at("/workspaces/w1/board?create=1");
    mockFetch([]);

    render();

    expect(await screen.findByRole("dialog", { name: "Create story" })).toBeInTheDocument();
    expect(router.params.has("create")).toBe(false);
  });

  it("opens the story named in ?story= and complains about an unknown one", async () => {
    router.at("/workspaces/w1/board?story=pru-2");
    mockFetch(drawerOf("PRU-2"));
    const first = render();

    expect(await drawer("PRU-2")).toBeInTheDocument();
    first.unmount();

    router.at("/workspaces/w1/board?story=PRU-404");
    render();

    expect(await screen.findByText("We could not find what you were looking for.")).toBeInTheDocument();
    expect(router.params.has("story")).toBe(false);
  });

  it("moves a story from the drawer to another column", async () => {
    mockFetch([...drawerOf("PRU-1"), { method: "PUT", path: `${BASE}/stories/PRU-1/move`, body: { ...login, columnId: "doing" } }]);
    const { user } = render();

    await user.click(within(column("To Do")).getByText("Login page"));
    const opened = await drawer("PRU-1");
    await user.click(within(within(opened).getByRole("group", { name: "Status" })).getByRole("button", { name: "In Progress" }));

    await waitFor(() => expect(within(column("In Progress")).getByText("Login page")).toBeInTheDocument());
    expect(within(column("To Do")).queryByText("Login page")).not.toBeInTheDocument();
  });

  it("syncs the pull requests, reloads the board and lists the repositories that failed", async () => {
    const broken = repository({ fullName: "arthur/prumo", lastSyncError: "RATE_LIMITED" });
    const refreshed: Board = board({ stories: [login, reset, { ...deploy, title: "Deploy (merged)" }] });
    const fetch = mockFetch([
      { method: "POST", path: `${BASE}/repositories/sync`, body: [broken] },
      { method: "GET", path: `${BASE}/board`, body: refreshed },
      { method: "GET", path: `${BASE}/repositories`, body: [broken] },
    ]);
    const { user } = render({ repositories: [repository()] });

    await user.click(screen.getByRole("button", { name: "Sync pull requests" }));

    expect(await screen.findByText("Pull requests updated")).toBeInTheDocument();
    expect(await screen.findByText(/^arthur\/prumo: /)).toBeInTheDocument();
    expect(await within(column("Done")).findByText("Deploy (merged)")).toBeInTheDocument();
    expect(requests(fetch, "GET", `${BASE}/board`)).toHaveLength(1);
  });

  it("deleting a column with stories asks where they go", async () => {
    const fetch = mockFetch([
      { method: "DELETE", path: `${BASE}/board/columns/todo`, status: 204 },
      { method: "GET", path: `${BASE}/board`, body: board({ columns: board().columns.slice(1), stories: [{ ...login, columnId: "done" }] }) },
      { method: "GET", path: `${BASE}/repositories`, body: [] },
    ]);
    const { user } = render();

    await user.click(within(column("To Do")).getByRole("button", { name: "Column options" }));
    await user.click(await screen.findByRole("menuitem", { name: "Delete column" }));
    const dialog = screen.getByRole("dialog", { name: "Delete To Do?" });
    expect(dialog).toHaveTextContent("This column has stories.");
    await user.selectOptions(within(dialog).getByRole("combobox", { name: "Move stories to" }), "Done");
    await user.click(within(dialog).getByRole("button", { name: "Delete column" }));

    await waitFor(() => expect(screen.queryByRole("region", { name: "To Do" })).not.toBeInTheDocument());
    expect(requests(fetch, "DELETE")[0].url.searchParams.get("moveTo")).toBe("done");
  });

  it("only owners and admins see the column tools", () => {
    mockFetch([]);

    render({ canManageBoard: false });

    expect(screen.queryByRole("button", { name: "Add column" })).not.toBeInTheDocument();
    expect(within(column("To Do")).queryByRole("button", { name: "Column options" })).not.toBeInTheDocument();
  });

  it("closing a native dialog with Escape closes the create form", async () => {
    mockFetch([]);
    const { user } = render();

    await user.click(createButton());
    fireEvent(screen.getByRole("dialog", { name: "Create story" }), new Event("close"));

    expect(screen.queryByRole("dialog", { name: "Create story" })).not.toBeInTheDocument();
  });

  it("keeps the order of the cards inside a column", () => {
    mockFetch([]);

    render();

    expect(within(column("To Do")).getAllByText(/^(Login page|Reset password)$/).map((title) => title.textContent)).toEqual([
      "Login page",
      "Reset password",
    ]);
  });
});
