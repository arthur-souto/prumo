import { NextResponse } from "next/server";
import { backendFetch, jsonRequest } from "@/lib/backend";
import { clearSession, readRefreshToken } from "@/lib/cookies";

export async function POST() {
  const refreshToken = await readRefreshToken();

  if (refreshToken) {
    await backendFetch("/api/v1/auth/logout", jsonRequest({ refresh_token: refreshToken })).catch(() => null);
  }

  return clearSession(NextResponse.json({ ok: true }));
}
