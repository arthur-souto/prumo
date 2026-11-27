import { useSyncExternalStore } from "react";
import { vi } from "vitest";

/**
 * A fake of next/navigation. Like the real App Router, the hooks follow window.history, so code that calls
 * history.replaceState (see lib/url-state.ts) re-renders with the new search params.
 *
 * Tests set the current URL with {@link router.at} and assert on the spies,
 * e.g. `expect(router.push).toHaveBeenCalledWith("/workspaces/1/board")`.
 */
export const router = {
  push: vi.fn(),
  replace: vi.fn(),
  refresh: vi.fn(),
  back: vi.fn(),
  forward: vi.fn(),
  prefetch: vi.fn(),
  at(path: string) {
    window.history.replaceState(null, "", path);
  },
  /** The current search params, to assert on what the page wrote to the URL. */
  get params() {
    return new URLSearchParams(window.location.search);
  },
  reset() {
    for (const spy of [router.push, router.replace, router.refresh, router.back, router.forward, router.prefetch]) {
      spy.mockReset();
    }
    window.history.replaceState(null, "", "/");
  },
};

const listeners = new Set<() => void>();

for (const method of ["pushState", "replaceState"] as const) {
  const original = window.history[method].bind(window.history);

  window.history[method] = (...args: Parameters<History["pushState"]>) => {
    original(...args);
    listeners.forEach((listener) => listener());
  };
}

function subscribe(listener: () => void) {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

// one object per query string, so hooks that depend on the params only re-run when the URL really changes
let cached = { search: "", params: new URLSearchParams() };

function currentParams() {
  if (cached.search !== window.location.search) {
    cached = { search: window.location.search, params: new URLSearchParams(window.location.search) };
  }

  return cached.params;
}

export function useRouter() {
  return router;
}

export function usePathname() {
  return useSyncExternalStore(subscribe, () => window.location.pathname);
}

export function useSearchParams() {
  return useSyncExternalStore(subscribe, currentParams);
}

export function useParams() {
  return {};
}

export function redirect(path: string): never {
  throw new Error(`redirect(${path})`);
}

export function notFound(): never {
  throw new Error("notFound()");
}
