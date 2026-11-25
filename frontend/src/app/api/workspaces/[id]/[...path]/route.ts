import { NextResponse } from "next/server";
import { proxyToBackend } from "@/lib/proxy";

type Context = { params: Promise<{ id: string; path: string[] }> };

const SEGMENT = /^[A-Za-z0-9._-]+$/;
const ALLOWED_ROOTS = new Set(["board", "stories", "subtasks", "repositories", "comments", "sprints"]);

async function forward(request: Request, { params }: Context) {
  const { id, path } = await params;

  if (!SEGMENT.test(id) || !ALLOWED_ROOTS.has(path[0]) || !path.every((segment) => SEGMENT.test(segment))) {
    return NextResponse.json({ code: "notFound" }, { status: 404 });
  }

  const search = new URL(request.url).search;
  const hasBody = request.method !== "GET" && request.method !== "DELETE";
  const body = hasBody ? ((await request.json().catch(() => null)) ?? {}) : undefined;

  return proxyToBackend(`/api/v1/workspaces/${id}/${path.join("/")}${search}`, request.method, body);
}

export const GET = forward;
export const POST = forward;
export const PUT = forward;
export const PATCH = forward;
export const DELETE = forward;
