import { NextResponse } from "next/server";
import { backendFetch, jsonRequest, type TokenPayload } from "@/lib/backend";
import { writeSession } from "@/lib/cookies";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);

  if (!body?.email || !body?.password) {
    return NextResponse.json({ message: "Enter your email and password." }, { status: 400 });
  }

  const response = await backendFetch(
    "/api/v1/auth/login",
    jsonRequest({ email: body.email, password: body.password }),
  ).catch(() => null);

  if (!response) {
    return NextResponse.json({ message: "The service is unavailable. Try again shortly." }, { status: 502 });
  }

  if (response.status === 401) {
    return NextResponse.json({ message: "Incorrect email or password." }, { status: 401 });
  }

  if (!response.ok) {
    return NextResponse.json({ message: "We could not log you in. Check the fields and try again." }, { status: 400 });
  }

  return writeSession(NextResponse.json({ ok: true }), (await response.json()) as TokenPayload);
}
