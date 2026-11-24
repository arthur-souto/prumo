export type ApiResult<T> = {
  ok: boolean;
  status: number;
  data: T | null;
  code: string;
  reason?: string;
  fields: Record<string, string>;
};

export async function sendJson<T>(url: string, method: string, body?: unknown): Promise<ApiResult<T>> {
  const response = await fetch(url, {
    method,
    headers: body === undefined ? undefined : { "Content-Type": "application/json" },
    body: body === undefined ? undefined : JSON.stringify(body),
  }).catch(() => null);

  if (response?.status === 401) {
    window.location.assign(`/api/auth/refresh?next=${encodeURIComponent(window.location.pathname)}`);
  }

  const data = response && response.status !== 204 ? await response.json().catch(() => null) : null;

  return {
    ok: Boolean(response?.ok),
    status: response?.status ?? 0,
    data: response?.ok ? (data as T) : null,
    code: response ? (data?.code ?? "generic") : "unavailable",
    reason: data?.reason,
    fields: data?.fields ?? {},
  };
}
