import { NextResponse } from "next/server";
import { backendFetch, jsonRequest, type TokenPayload } from "@/lib/backend";
import { writeSession } from "@/lib/cookies";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);

  if (!body?.email || !body?.password) {
    return NextResponse.json({ code: "enterCredentials" }, { status: 400 });
  }

  const response = await backendFetch(
    "/api/v1/auth/login",
    jsonRequest({ email: body.email, password: body.password }),
  ).catch(() => null);

  if (!response) {
    return NextResponse.json({ code: "unavailable" }, { status: 502 });
  }

  if (response.status === 401) {
    return NextResponse.json({ code: "invalidCredentials" }, { status: 401 });
  }

  if (!response.ok) {
    return NextResponse.json({ code: "invalid" }, { status: 400 });
  }

  return writeSession(NextResponse.json({ ok: true }), (await response.json()) as TokenPayload);
}
