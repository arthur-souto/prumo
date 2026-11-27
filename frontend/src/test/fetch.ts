import { vi } from "vitest";

type Answer = { status?: number; body?: unknown };

type Route = Answer & {
  method: string;
  path: string;
  /** Extra condition on the full URL, e.g. a query parameter. */
  when?: (url: URL) => boolean;
  /** Answers given in turn on each call; the last one repeats. Overrides status and body. */
  answers?: Answer[];
};

/**
 * Replaces fetch with canned answers, matched by method and path (and `when`, if given).
 * Unmatched calls fail the test loudly instead of hanging. Returns the spy to assert on the calls.
 */
export function mockFetch(routes: Route[]) {
  const calls = new Map<Route, number>();

  const spy = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = new URL(String(input), "http://localhost");
    const method = (init?.method ?? "GET").toUpperCase();
    const route = routes.find(
      (candidate) => candidate.method === method && candidate.path === url.pathname && (candidate.when?.(url) ?? true),
    );

    if (!route) {
      throw new Error(`Unexpected ${method} ${url.pathname}${url.search}`);
    }

    const count = calls.get(route) ?? 0;
    calls.set(route, count + 1);
    const answer = route.answers ? route.answers[Math.min(count, route.answers.length - 1)] : route;
    const status = answer.status ?? 200;

    return new Response(status === 204 || answer.body === undefined ? null : JSON.stringify(answer.body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  });

  vi.stubGlobal("fetch", spy);

  return spy;
}

export type FetchSpy = ReturnType<typeof mockFetch>;

/** The parsed JSON body sent on the nth call of a fetch spy. */
export function sentBody(spy: FetchSpy, call = 0) {
  const init = spy.mock.calls[call]?.[1];

  return init?.body ? JSON.parse(String(init.body)) : undefined;
}

/** The calls made to a method and path, as "METHOD /path?query" strings in order. */
export function requests(spy: FetchSpy, method?: string, path?: string) {
  return spy.mock.calls
    .map(([input, init]) => {
      const url = new URL(String(input), "http://localhost");
      return { method: (init?.method ?? "GET").toUpperCase(), url, body: init?.body ? JSON.parse(String(init.body)) : undefined };
    })
    .filter((call) => (!method || call.method === method) && (!path || call.url.pathname === path));
}
