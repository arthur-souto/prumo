import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterEach, vi } from "vitest";
import { router } from "./navigation";

// Client components read the App Router through next/navigation, which only works inside a real Next request.
// Every test gets the same controllable fake; see ./navigation.ts.
vi.mock("next/navigation", () => import("./navigation"));

afterEach(() => {
  cleanup();
  router.reset();
});
