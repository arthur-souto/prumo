import { NextResponse, type NextRequest } from "next/server";
import { backendFetch, jsonRequest, type TokenPayload } from "@/lib/backend";
import { writeSession } from "@/lib/cookies";

function failure(request: NextRequest, reason: string) {
  const url = new URL("/login", request.url);
  url.searchParams.set("error", reason);
  return NextResponse.redirect(url);
}

export async function GET(request: NextRequest) {
  const params = request.nextUrl.searchParams;
  const error = params.get("error");
  const code = params.get("code");
  const linked = params.get("linked");

  if (error) {
    return failure(request, error);
  }

  if (linked) {
    return NextResponse.redirect(new URL("/", request.url));
  }

  if (!code) {
    return failure(request, "oauth_failed");
  }

  const response = await backendFetch("/api/v1/auth/oauth/exchange", jsonRequest({ code })).catch(() => null);

  if (!response?.ok) {
    return failure(request, "oauth_failed");
  }

  return writeSession(NextResponse.redirect(new URL("/", request.url)), (await response.json()) as TokenPayload);
}
