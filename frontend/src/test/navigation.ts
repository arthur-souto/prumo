import { vi } from "vitest";

/**
 * A fake of next/navigation. Tests set the current URL with {@link router.at} and assert on the spies,
 * e.g. `expect(router.push).toHaveBeenCalledWith("/workspaces/1/board")`.
 */
export const router = {
  push: vi.fn(),
  replace: vi.fn(),
  refresh: vi.fn(),
  back: vi.fn(),
  forward: vi.fn(),
  prefetch: vi.fn(),
  url: new URL("http://localhost/"),
  at(path: string) {
    router.url = new URL(path, "http://localhost");
  },
  reset() {
    for (const spy of [router.push, router.replace, router.refresh, router.back, router.forward, router.prefetch]) {
      spy.mockReset();
    }
    router.url = new URL("http://localhost/");
  },
};

export function useRouter() {
  return router;
}

export function usePathname() {
  return router.url.pathname;
}

export function useSearchParams() {
  return router.url.searchParams;
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
