import { NextResponse } from "next/server";

/**
 * Redirects with a relative Location, so the browser resolves it against the address it actually used.
 * `request.url` can't be trusted for this: when the server listens on all interfaces (`npm run dev:lan`)
 * it reports the bind address (0.0.0.0), which is useless to another machine.
 */
export function redirectTo(path: string) {
  return new NextResponse(null, { status: 307, headers: { Location: path } });
}
