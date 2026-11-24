import { NextResponse, type NextRequest } from "next/server";
import { clearSession, readRefreshToken, writeSession } from "@/lib/cookies";
import { refreshTokens } from "@/lib/refresh";
import { safeRedirectPath } from "@/lib/session";

export async function GET(request: NextRequest) {
  const loginUrl = new URL("/login", request.url);
  const refreshToken = await readRefreshToken();

  if (request.nextUrl.searchParams.has("clear") || !refreshToken) {
    return clearSession(NextResponse.redirect(loginUrl));
  }

  const result = await refreshTokens(refreshToken);

  if (result.kind !== "renewed") {
    return clearSession(NextResponse.redirect(loginUrl));
  }

  const next = new URL(safeRedirectPath(request.nextUrl.searchParams.get("next")), request.url);
  next.searchParams.set("refreshed", "1");

  return writeSession(NextResponse.redirect(next), result.tokens);
}
