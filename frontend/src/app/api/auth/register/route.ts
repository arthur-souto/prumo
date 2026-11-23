import { NextResponse } from "next/server";
import { backendFetch, jsonRequest, type TokenPayload } from "@/lib/backend";
import { writeSession } from "@/lib/cookies";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);

  if (!body) {
    return NextResponse.json({ code: "invalid" }, { status: 400 });
  }

  const credentials = { email: body.email, password: body.password };

  const registration = await backendFetch(
    "/api/v1/auth/register",
    jsonRequest({ name: body.name, ...credentials }),
  ).catch(() => null);

  if (!registration) {
    return NextResponse.json({ code: "unavailable" }, { status: 502 });
  }

  if (registration.status === 409) {
    return NextResponse.json({ code: "emailTaken" }, { status: 409 });
  }

  if (!registration.ok) {
    const problem = await registration.json().catch(() => null);
    const fields: Record<string, string> = {};

    for (const error of problem?.errors ?? []) {
      fields[error.field] = error.message;
    }

    return NextResponse.json({ code: "fixFields", fields }, { status: 400 });
  }

  const login = await backendFetch("/api/v1/auth/login", jsonRequest(credentials)).catch(() => null);

  if (!login?.ok) {
    return NextResponse.json({ ok: true, loggedIn: false });
  }

  return writeSession(NextResponse.json({ ok: true, loggedIn: true }), (await login.json()) as TokenPayload);
}
