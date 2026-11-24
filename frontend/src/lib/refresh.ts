import { internalBackendUrl } from "./backend-url";
import type { TokenPayload } from "./backend";

export type RefreshResult =
  | { kind: "renewed"; tokens: TokenPayload }
  | { kind: "rejected" }
  | { kind: "unavailable" };

/**
 * The backend rotates the refresh token on every use and revokes the whole family if an old one comes back.
 * Several requests can reach us with the same expired session at once (layout, page, prefetches, other tabs),
 * so the first call goes to the backend and everyone holding that same token shares its outcome for a short while.
 */
const SHARE_OUTCOME_MS = 30_000;
const attempts = new Map<string, Promise<RefreshResult>>();

async function callBackend(refreshToken: string): Promise<RefreshResult> {
  try {
    const response = await fetch(`${internalBackendUrl}/api/v1/auth/refresh`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refresh_token: refreshToken }),
      cache: "no-store",
    });

    if (response.ok) {
      return { kind: "renewed", tokens: (await response.json()) as TokenPayload };
    }

    return response.status >= 400 && response.status < 500 ? { kind: "rejected" } : { kind: "unavailable" };
  } catch {
    return { kind: "unavailable" };
  }
}

export function refreshTokens(refreshToken: string): Promise<RefreshResult> {
  const existing = attempts.get(refreshToken);

  if (existing) {
    return existing;
  }

  const attempt = callBackend(refreshToken).then((result) => {
    if (result.kind === "unavailable") {
      attempts.delete(refreshToken);
    } else {
      setTimeout(() => attempts.delete(refreshToken), SHARE_OUTCOME_MS).unref?.();
    }

    return result;
  });

  attempts.set(refreshToken, attempt);
  return attempt;
}
