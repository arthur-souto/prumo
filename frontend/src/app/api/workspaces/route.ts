import { proxyToBackend } from "@/lib/proxy";

export async function POST(request: Request) {
  const body = await request.json().catch(() => null);

  return proxyToBackend("/api/v1/workspaces", "POST", body ?? {});
}
