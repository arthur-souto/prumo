import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test/render";
import { Drawer } from "./Drawer";

describe("Drawer", () => {
  it("is announced by what it is and by its visible title", () => {
    renderWithProviders(
      <Drawer open label="Story details" title={<span>PRU-2</span>} onClose={vi.fn()}>
        Body
      </Drawer>,
    );

    const dialog = screen.getByRole("dialog", { name: "Story details: PRU-2" });
    // the label is for assistive tech only; sighted users see just the key
    expect(screen.getByText("PRU-2")).toBeVisible();
    expect(dialog).not.toHaveAttribute("aria-label");
  });

  it("closes with the close button and with Escape", async () => {
    const onClose = vi.fn();
    const { user } = renderWithProviders(
      <Drawer open label="Story details" title="PRU-2" onClose={onClose}>
        Body
      </Drawer>,
    );

    await user.click(screen.getByRole("button", { name: "Close" }));
    await user.keyboard("{Escape}");

    expect(onClose).toHaveBeenCalledTimes(2);
  });
});
