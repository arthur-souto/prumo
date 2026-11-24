import { NextResponse, type NextRequest } from "next/server";
import { ACCESS_COOKIE, REFRESH_COOKIE, clearSession, writeSession } from "@/lib/cookies";
import { refreshTokens } from "@/lib/refresh";

const RENEW_WHEN_SECONDS_LEFT = 60;

function secondsLeft(accessToken: string | undefined) {
  if (!accessToken) return 0;

  try {
    const payload = JSON.parse(Buffer.from(accessToken.split(".")[1], "base64url").toString());
    return typeof payload.exp === "number" ? payload.exp - Date.now() / 1000 : 0;
  } catch {
    return 0;
  }
}

/**
 * Keeps the session alive before the page renders: when the access token is missing or about to expire it is renewed
 * here, the new cookies go out with the response and the page in this same request already sees them. Without this the
 * pages had to bounce through a redirect, and parallel bounces reused a rotated refresh token and ended in the login.
 */
export async function proxy(request: NextRequest) {
  const refreshToken = request.cookies.get(REFRESH_COOKIE)?.value;

  if (!refreshToken || secondsLeft(request.cookies.get(ACCESS_COOKIE)?.value) > RENEW_WHEN_SECONDS_LEFT) {
    return NextResponse.next();
  }

  const result = await refreshTokens(refreshToken);

  if (result.kind === "unavailable") {
    return NextResponse.next();
  }

  if (result.kind === "rejected") {
    request.cookies.delete(ACCESS_COOKIE);
    request.cookies.delete(REFRESH_COOKIE);
    return clearSession(NextResponse.next({ request }));
  }

  request.cookies.set(ACCESS_COOKIE, result.tokens.access_token);
  request.cookies.set(REFRESH_COOKIE, result.tokens.refresh_token);
  return writeSession(NextResponse.next({ request }), result.tokens);
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\..*).*)"],
};
