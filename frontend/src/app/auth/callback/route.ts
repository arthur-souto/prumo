import type { NextRequest } from "next/server";
import { backendFetch, jsonRequest, type TokenPayload } from "@/lib/backend";
import { writeSession } from "@/lib/cookies";
import { redirectTo } from "@/lib/redirect";

function failure(reason: string) {
  return redirectTo(`/login?error=${encodeURIComponent(reason)}`);
}

export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const error = params.get("error");
  const code = params.get("code");
  const linked = params.get("linked");

  if (error) {
    return failure(error);
  }

  if (linked) {
    return redirectTo("/");
  }

  if (!code) {
    return failure("oauth_failed");
  }

  const response = await backendFetch("/api/v1/auth/oauth/exchange", jsonRequest({ code })).catch(() => null);

  if (!response?.ok) {
    return failure("oauth_failed");
  }

  return writeSession(redirectTo("/"), (await response.json()) as TokenPayload);
}
