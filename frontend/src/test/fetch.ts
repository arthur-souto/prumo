import { vi } from "vitest";

type Route = { method: string; path: string; status?: number; body?: unknown };

/**
 * Replaces fetch with a list of canned answers, matched by method and path (query string ignored).
 * Unmatched calls fail the test loudly instead of hanging. Returns the spy to assert on the calls.
 */
export function mockFetch(routes: Route[]) {
  const spy = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), "http://localhost");
    const method = (init?.method ?? "GET").toUpperCase();
    const route = routes.find((candidate) => candidate.method === method && candidate.path === url.pathname);

    if (!route) {
      throw new Error(`Unexpected ${method} ${url.pathname}`);
    }

    const status = route.status ?? 200;

    return new Response(status === 204 || route.body === undefined ? null : JSON.stringify(route.body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  });

  vi.stubGlobal("fetch", spy);

  return spy;
}

/** The parsed JSON body sent on the nth call of a fetch spy. */
export function sentBody(spy: ReturnType<typeof mockFetch>, call = 0) {
  const init = spy.mock.calls[call]?.[1];

  return init?.body ? JSON.parse(String(init.body)) : undefined;
}
