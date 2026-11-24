import { NextResponse } from "next/server";
import { backendFetch } from "./backend";
import { readAccessToken } from "./cookies";

function codeFor(status: number) {
  switch (status) {
    case 400:
      return "fixFields";
    case 401:
      return "sessionExpired";
    case 403:
      return "forbidden";
    case 404:
      return "notFound";
    case 409:
      return "conflict";
    case 422:
      return "unprocessable";
    default:
      return "generic";
  }
}

export async function proxyToBackend(path: string, method: string, body?: unknown) {
  const token = await readAccessToken();

  if (!token) {
    return NextResponse.json({ code: "sessionExpired" }, { status: 401 });
  }

  const response = await backendFetch(path, {
    method,
    headers: {
      Authorization: `Bearer ${token}`,
      ...(body === undefined ? {} : { "Content-Type": "application/json" }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).catch(() => null);

  if (!response) {
    return NextResponse.json({ code: "unavailable" }, { status: 502 });
  }

  if (response.status === 204) {
    return new NextResponse(null, { status: 204 });
  }

  const payload = await response.json().catch(() => null);

  if (response.ok) {
    return NextResponse.json(payload, { status: response.status });
  }

  const fields: Record<string, string> = {};
  for (const error of payload?.errors ?? []) {
    fields[error.field] = error.message;
  }

  return NextResponse.json(
    { code: codeFor(response.status), reason: typeof payload?.code === "string" ? payload.code : undefined, fields },
    { status: response.status },
  );
}
