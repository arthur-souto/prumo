import { screen, waitFor, within } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { mockFetch, requests } from "@/test/fetch";
import { COLUMNS, member, repository, sprint, story } from "@/test/fixtures";
import { renderWithProviders } from "@/test/render";
import type { Story } from "@/lib/board-types";
import { StoryDrawer } from "./StoryDrawer";

const STORY = "/api/workspaces/w1/stories/PRU-1";
const ana = member({ name: "Ana" });
const active = sprint({ name: "Sprint 7", state: "ACTIVE" });
const closed = sprint({ name: "Sprint 6", state: "CLOSED" });

/** What the drawer loads on its own when it opens. */
const sections = [
  { method: "GET", path: `${STORY}/subtasks`, body: [] },
  { method: "GET", path: `${STORY}/comments`, body: [] },
  { method: "GET", path: `${STORY}/activity`, body: [] },
];

function render(overrides: Partial<Story> = {}, props: Partial<Parameters<typeof StoryDrawer>[0]> = {}) {
  const current = story({ key: "PRU-1", title: "Reset password", priority: "MEDIUM", labels: ["api"], storyPoints: 3, ...overrides });
  const handlers = {
    onChange: vi.fn(),
    onMoveToColumn: vi.fn().mockResolvedValue(true),
    onRepositoriesChanged: vi.fn().mockResolvedValue(undefined),
    onDeleted: vi.fn(),
    onClose: vi.fn(),
  };
  const view = renderWithProviders(
    <StoryDrawer
      workspaceId="w1"
      story={current}
      columns={COLUMNS}
      members={[ana]}
      sprints={[active, closed]}
      repositories={[]}
      hasGithubToken={false}
      {...handlers}
      {...props}
    />,
  );

  return { ...view, ...handlers, current };
}

const titleField = () => screen.getByRole("textbox", { name: "Title" });
// the header X and the footer button both ask to close; the footer one is used here
const closeButton = () => screen.getAllByRole("button", { name: "Close" }).at(-1)!;

describe("StoryDrawer", () => {
  it("saves only after a change and sends the edited fields in the API's shape", async () => {
    const fetch = mockFetch([...sections, { method: "PUT", path: STORY, body: story({ key: "PRU-1", title: "Reset the password" }) }]);
    const { user, onChange } = render();
    const save = screen.getByRole("button", { name: "Save changes" });
    expect(save).toBeDisabled();

    await user.clear(titleField());
    await user.type(titleField(), "Reset the password");
    await user.clear(screen.getByRole("spinbutton"));
    await user.type(screen.getByRole("spinbutton"), "5");
    await user.clear(screen.getByPlaceholderText("Separate with commas"));
    await user.type(screen.getByPlaceholderText("Separate with commas"), "api, , auth ");
    await user.type(screen.getByPlaceholderText("Add a criterion"), "Email arrives{Enter}");
    expect(screen.getByText("Unsaved changes")).toBeInTheDocument();
    await user.click(save);

    expect(await screen.findByText("Changes saved")).toBeInTheDocument();
    expect(requests(fetch, "PUT")[0].body).toEqual({
      title: "Reset the password",
      description: "",
      priority: "MEDIUM",
      storyPoints: 5,
      labels: ["api", "auth"],
      acceptanceCriteria: [{ id: null, text: "Email arrives", done: false }],
    });
    expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ title: "Reset the password" }));
  });

  it("clearing the points sends null, not zero", async () => {
    const fetch = mockFetch([...sections, { method: "PUT", path: STORY, body: story({ key: "PRU-1" }) }]);
    const { user } = render();

    await user.clear(screen.getByRole("spinbutton"));
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    await waitFor(() => expect(requests(fetch, "PUT")).toHaveLength(1));
    expect(requests(fetch, "PUT")[0].body.storyPoints).toBeNull();
  });

  it("lets the browser refuse points above the limit before asking the API", async () => {
    const fetch = mockFetch(sections);
    const { user } = render();

    await user.type(screen.getByRole("spinbutton"), "0000");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    expect(screen.getByRole("spinbutton")).toBeInvalid();
    expect(requests(fetch, "PUT")).toHaveLength(0);
  });

  it("shows the field errors the API returns next to each field", async () => {
    mockFetch([...sections, { method: "PUT", path: STORY, status: 400, body: { code: "invalid", fields: { labels: "at most 20 labels" } } }]);
    const { user } = render();

    await user.type(screen.getByPlaceholderText("Separate with commas"), ", more");
    await user.click(screen.getByRole("button", { name: "Save changes" }));

    expect(await screen.findByText("at most 20 labels")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Separate with commas")).toHaveAttribute("aria-invalid", "true");
    // a field error replaces the generic banner
    expect(screen.queryByText("Check the fields and try again.")).not.toBeInTheDocument();
  });

  it("asks before throwing away unsaved changes", async () => {
    mockFetch(sections);
    const { user, onClose } = render();

    await user.type(titleField(), "!");
    await user.click(closeButton());
    const confirm = screen.getByRole("dialog", { name: "Discard your changes?" });
    await user.click(within(confirm).getByRole("button", { name: "Keep editing" }));
    expect(onClose).not.toHaveBeenCalled();
    expect(titleField()).toHaveValue("Reset password!");

    await user.click(closeButton());
    await user.click(within(screen.getByRole("dialog", { name: "Discard your changes?" })).getByRole("button", { name: "Discard" }));
    expect(onClose).toHaveBeenCalledOnce();
  });

  it("closes right away when nothing changed", async () => {
    mockFetch(sections);
    const { user, onClose } = render();

    await user.click(closeButton());

    expect(onClose).toHaveBeenCalledOnce();
    expect(screen.queryByRole("dialog", { name: "Discard your changes?" })).not.toBeInTheDocument();
  });

  it("moves the story with the status buttons and confirms where it went", async () => {
    mockFetch(sections);
    const { user, onMoveToColumn, current } = render({ columnId: "todo" });
    const status = screen.getByRole("group", { name: "Status" });
    expect(within(status).getByRole("button", { name: "To Do" })).toHaveAttribute("aria-pressed", "true");

    await user.click(within(status).getByRole("button", { name: "To Do" }));
    expect(onMoveToColumn).not.toHaveBeenCalled();
    await user.click(within(status).getByRole("button", { name: "In Review" }));

    expect(onMoveToColumn).toHaveBeenCalledWith(current, "review");
    expect(await screen.findByText("Moved to In Review")).toBeInTheDocument();
  });

  it("assigns and plans right away, and closed sprints cannot be picked", async () => {
    const fetch = mockFetch([
      ...sections,
      { method: "PUT", path: `${STORY}/assignee`, body: story({ key: "PRU-1", assigneeId: ana.id }) },
      { method: "PUT", path: `${STORY}/sprint`, body: story({ key: "PRU-1", sprintId: active.id }) },
    ]);
    const { user, onChange } = render();

    await user.selectOptions(screen.getByRole("combobox", { name: "Assignee" }), "Ana");
    await user.selectOptions(screen.getByRole("combobox", { name: "Sprint" }), "Sprint 7 · Active");

    await waitFor(() => expect(onChange).toHaveBeenCalledTimes(2));
    expect(requests(fetch, "PUT").map((call) => call.body)).toEqual([{ assigneeId: ana.id }, { sprintId: active.id }]);
    expect(screen.queryByRole("option", { name: "Sprint 6" })).not.toBeInTheDocument();
  });

  it("deletes from the menu after a confirmation", async () => {
    const fetch = mockFetch([...sections, { method: "DELETE", path: STORY, status: 204 }]);
    const { user, onDeleted } = render();

    await user.click(screen.getByRole("button", { name: "More actions" }));
    await user.click(await screen.findByRole("menuitem", { name: "Delete story" }));
    await user.click(within(screen.getByRole("dialog", { name: "Delete PRU-1?" })).getByRole("button", { name: "Delete story" }));

    await waitFor(() => expect(onDeleted).toHaveBeenCalledOnce());
    expect(requests(fetch, "DELETE")).toHaveLength(1);
  });

  it("keeps the drawer open and explains when the delete is refused", async () => {
    mockFetch([...sections, { method: "DELETE", path: STORY, status: 403, body: { code: "forbidden" } }]);
    const { user, onDeleted } = render();

    await user.click(screen.getByRole("button", { name: "More actions" }));
    await user.click(await screen.findByRole("menuitem", { name: "Delete story" }));
    await user.click(within(screen.getByRole("dialog", { name: "Delete PRU-1?" })).getByRole("button", { name: "Delete story" }));

    expect(await screen.findByText("You do not have permission to do this.")).toBeInTheDocument();
    expect(onDeleted).not.toHaveBeenCalled();
  });

  it("points to the settings when GitHub is not set up, and links a repository when it is", async () => {
    mockFetch(sections);
    const first = render();
    expect(screen.getByRole("link", { name: "Settings" })).toHaveAttribute("href", "/settings");
    first.unmount();

    const fetch = mockFetch([...sections, { method: "PUT", path: `${STORY}/repository`, body: story({ key: "PRU-1", repositoryId: "repo-1" }) }]);
    const { user, onChange } = render({}, { repositories: [repository()], hasGithubToken: true });

    await user.selectOptions(screen.getByRole("combobox", { name: "Repository" }), "arthur/prumo");

    await waitFor(() => expect(onChange).toHaveBeenCalledWith(expect.objectContaining({ repositoryId: "repo-1" })));
    expect(requests(fetch, "PUT")[0].body).toEqual({ repositoryId: "repo-1" });
  });
});
