import type { NextRequest } from "next/server";
import { clearSession, readRefreshToken, writeSession } from "@/lib/cookies";
import { redirectTo } from "@/lib/redirect";
import { refreshTokens } from "@/lib/refresh";
import { safeRedirectPath } from "@/lib/session";

export async function GET(request: NextRequest) {
  const refreshToken = await readRefreshToken();

  if (request.nextUrl.searchParams.has("clear") || !refreshToken) {
    return clearSession(redirectTo("/login"));
  }

  const result = await refreshTokens(refreshToken);

  if (result.kind !== "renewed") {
    return clearSession(redirectTo("/login"));
  }

  const next = new URL(safeRedirectPath(request.nextUrl.searchParams.get("next")), "http://placeholder");
  next.searchParams.set("refreshed", "1");

  return writeSession(redirectTo(`${next.pathname}${next.search}${next.hash}`), result.tokens);
}
