import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { Logo } from "./Logo";

describe("Logo", () => {
  it("reads as the name only, with one heron for each theme", () => {
    const { container } = render(<Logo />);

    expect(screen.getByText("Prumo")).toBeInTheDocument();
    expect(screen.queryByRole("img")).toBeNull();
    expect([...container.querySelectorAll("img")].map((img) => img.getAttribute("src"))).toEqual([
      "/brand/prumo-garca.svg",
      "/brand/prumo-garca-dark.svg",
    ]);
  });

  it("keeps the name in the text when it sits under the heron", () => {
    render(<Logo size="lg" layout="stacked" />);

    // the capitals come from CSS, so screen readers still read "Prumo"
    expect(screen.getByText("Prumo")).toBeInTheDocument();
    expect(screen.queryByRole("img")).toBeNull();
  });
});
