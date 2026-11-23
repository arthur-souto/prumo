import { backendFetch, type CurrentUser } from "./backend";
import { readAccessToken, readRefreshToken } from "./cookies";

export type Session =
  | { kind: "authenticated"; user: CurrentUser }
  | { kind: "expired" }
  | { kind: "anonymous" };

export async function getSession(): Promise<Session> {
  const accessToken = await readAccessToken();

  if (accessToken) {
    const response = await backendFetch("/api/v1/users/me", {
      headers: { Authorization: `Bearer ${accessToken}` },
    }).catch(() => null);

    if (response?.ok) {
      return { kind: "authenticated", user: (await response.json()) as CurrentUser };
    }
  }

  return (await readRefreshToken()) ? { kind: "expired" } : { kind: "anonymous" };
}

export function safeRedirectPath(value: string | null) {
  return value && value.startsWith("/") && !value.startsWith("//") ? value : "/";
}
