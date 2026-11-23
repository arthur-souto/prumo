export type WorkspaceRole = "OWNER" | "ADMIN" | "MEMBER";

export type Workspace = {
  id: string;
  name: string;
  slug: string;
  description: string | null;
  role: WorkspaceRole;
  memberCount: number;
  createdAt: string;
};

export function formatDate(iso: string, locale: string) {
  return new Date(iso).toLocaleDateString(locale, {
    year: "numeric",
    month: "short",
    day: "numeric",
    timeZone: "UTC",
  });
}
