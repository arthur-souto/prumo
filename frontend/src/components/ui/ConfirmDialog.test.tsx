import { screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { renderWithProviders } from "@/test/render";
import { ConfirmDialog } from "./ConfirmDialog";

function dialog(overrides: Partial<Parameters<typeof ConfirmDialog>[0]> = {}) {
  const props = {
    open: true,
    title: "Delete PRU-7?",
    description: "This cannot be undone.",
    confirmLabel: "Delete",
    onConfirm: vi.fn(),
    onCancel: vi.fn(),
    ...overrides,
  };

  // Portuguese, so the default cancel label is checked as translated text
  return { props, ...renderWithProviders(<ConfirmDialog {...props} />, { locale: "pt" }) };
}

describe("ConfirmDialog", () => {
  it("is an accessible dialog with the title and the description", () => {
    dialog();

    const element = screen.getByRole("dialog", { name: "Delete PRU-7?" });
    expect(element).toHaveAccessibleDescription("This cannot be undone.");
  });

  it("confirms with the confirm button", async () => {
    const { props, user } = dialog();

    await user.click(screen.getByRole("button", { name: "Delete" }));

    expect(props.onConfirm).toHaveBeenCalledOnce();
    expect(props.onCancel).not.toHaveBeenCalled();
  });

  it("cancels with the translated cancel button and with Escape", async () => {
    const { props, user } = dialog();

    await user.click(screen.getByRole("button", { name: "Cancelar" }));
    await user.keyboard("{Escape}");

    expect(props.onCancel).toHaveBeenCalledTimes(2);
    expect(props.onConfirm).not.toHaveBeenCalled();
  });

  it("blocks a second confirmation while busy", async () => {
    const { props, user } = dialog({ busy: true });

    const confirm = screen.getByRole("button", { name: "Delete" });
    expect(confirm).toBeDisabled();
    await user.click(confirm);

    expect(props.onConfirm).not.toHaveBeenCalled();
  });

  it("renders nothing when closed", () => {
    dialog({ open: false });

    expect(screen.queryByRole("dialog")).not.toBeInTheDocument();
  });
});
