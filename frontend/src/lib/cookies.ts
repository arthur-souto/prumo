import { cookies } from "next/headers";
import type { NextResponse } from "next/server";
import type { TokenPayload } from "./backend";

export const ACCESS_COOKIE = "prumo_at";
export const REFRESH_COOKIE = "prumo_rt";

const REFRESH_MAX_AGE_SECONDS = 60 * 60 * 24 * 7;
const secure = process.env.NODE_ENV === "production";

export function writeSession(response: NextResponse, tokens: TokenPayload) {
  response.cookies.set(ACCESS_COOKIE, tokens.access_token, {
    httpOnly: true,
    secure,
    sameSite: "lax",
    path: "/",
    maxAge: tokens.expires_in,
  });
  response.cookies.set(REFRESH_COOKIE, tokens.refresh_token, {
    httpOnly: true,
    secure,
    sameSite: "lax",
    path: "/",
    maxAge: REFRESH_MAX_AGE_SECONDS,
  });
  return response;
}

export function clearSession(response: NextResponse) {
  response.cookies.set(ACCESS_COOKIE, "", { path: "/", maxAge: 0 });
  response.cookies.set(REFRESH_COOKIE, "", { path: "/", maxAge: 0 });
  return response;
}

export async function readAccessToken() {
  return (await cookies()).get(ACCESS_COOKIE)?.value;
}

export async function readRefreshToken() {
  return (await cookies()).get(REFRESH_COOKIE)?.value;
}
