import { redirectTo } from "@/lib/redirect";

/** Same origin as the page: next.config.ts proxies /oauth2 to the backend, so this works from any machine. */
export function GET() {
  return redirectTo("/oauth2/authorization/github");
}
