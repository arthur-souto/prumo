import { proxyToBackend } from "@/lib/proxy";

type Context = { params: Promise<{ id: string }> };

export async function PUT(request: Request, { params }: Context) {
  const { id } = await params;
  const body = await request.json().catch(() => null);

  return proxyToBackend(`/api/v1/workspaces/${encodeURIComponent(id)}`, "PUT", body ?? {});
}

export async function DELETE(_request: Request, { params }: Context) {
  const { id } = await params;

  return proxyToBackend(`/api/v1/workspaces/${encodeURIComponent(id)}`, "DELETE");
}
