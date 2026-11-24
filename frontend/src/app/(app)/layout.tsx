import type { ReactNode } from "react";
import { AppShell } from "@/components/shell/AppShell";
import { getSession } from "@/lib/session";
import { listWorkspaces } from "@/lib/workspaces";

/**
 * The shell (top bar and sidebar) lives here so it stays on screen while a page streams in.
 * When the session is not usable the pages themselves redirect, so the layout just gets out of the way.
 */
export default async function AppLayout({ children }: { children: ReactNode }) {
  const session = await getSession();

  if (session.kind !== "authenticated") {
    return children;
  }

  const workspaces = await listWorkspaces();

  return (
    <AppShell user={session.user} workspaces={workspaces.map(({ id, name }) => ({ id, name }))}>
      {children}
    </AppShell>
  );
}
