import { NextResponse, type NextRequest } from "next/server";
import { backendFetch, jsonRequest, type TokenPayload } from "@/lib/backend";
import { clearSession, readRefreshToken, writeSession } from "@/lib/cookies";
import { safeRedirectPath } from "@/lib/session";

export async function GET(request: NextRequest) {
  const loginUrl = new URL("/login", request.url);
  const refreshToken = await readRefreshToken();

  if (request.nextUrl.searchParams.has("clear") || !refreshToken) {
    return clearSession(NextResponse.redirect(loginUrl));
  }

  const response = await backendFetch("/api/v1/auth/refresh", jsonRequest({ refresh_token: refreshToken })).catch(
    () => null,
  );

  if (!response?.ok) {
    return clearSession(NextResponse.redirect(loginUrl));
  }

  const next = new URL(safeRedirectPath(request.nextUrl.searchParams.get("next")), request.url);
  next.searchParams.set("refreshed", "1");

  return writeSession(NextResponse.redirect(next), (await response.json()) as TokenPayload);
}
