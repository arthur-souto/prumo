const internalUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

export const publicBackendUrl = process.env.BACKEND_PUBLIC_URL ?? internalUrl;

export function backendFetch(path: string, init: RequestInit = {}) {
  return fetch(`${internalUrl}${path}`, { ...init, cache: "no-store" });
}

export type TokenPayload = {
  access_token: string;
  expires_in: number;
  refresh_token: string;
};

export type CurrentUser = {
  id: string;
  name: string;
  email: string;
  role: string;
  avatarUrl: string | null;
};

export function jsonRequest(body: unknown): RequestInit {
  return {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(body),
  };
}
