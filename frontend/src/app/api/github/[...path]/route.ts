import { NextResponse } from "next/server";
import { proxyToBackend } from "@/lib/proxy";

type Context = { params: Promise<{ path: string[] }> };

const ALLOWED = new Set(["token", "connection", "repositories"]);

async function forward(request: Request, { params }: Context) {
  const { path } = await params;

  if (path.length !== 1 || !ALLOWED.has(path[0])) {
    return NextResponse.json({ code: "notFound" }, { status: 404 });
  }

  const hasBody = request.method === "PUT" || request.method === "POST";
  const body = hasBody ? ((await request.json().catch(() => null)) ?? {}) : undefined;

  return proxyToBackend(`/api/v1/github/${path[0]}`, request.method, body);
}

export const GET = forward;
export const PUT = forward;
export const DELETE = forward;
