import { backendLanguage } from "@/i18n/core";
import { getLocale } from "@/i18n/server";

const internalUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

export const publicBackendUrl = process.env.BACKEND_PUBLIC_URL ?? internalUrl;

export async function backendFetch(path: string, init: RequestInit = {}) {
  const locale = await getLocale();
  const headers = new Headers(init.headers);
  headers.set("Accept-Language", backendLanguage(locale));

  return fetch(`${internalUrl}${path}`, { ...init, headers, cache: "no-store" });
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
