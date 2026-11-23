import type { Metadata } from "next";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { TopNav } from "./TopNav";
import styles from "./home.module.css";

export const metadata: Metadata = { title: "Your work" };

type HomeProps = {
  searchParams: Promise<{ refreshed?: string }>;
};

export default async function HomePage({ searchParams }: HomeProps) {
  const session = await getSession();

  if (session.kind === "anonymous") {
    redirect("/login");
  }

  if (session.kind === "expired") {
    const { refreshed } = await searchParams;
    redirect(refreshed ? "/api/auth/refresh?clear=1" : "/api/auth/refresh?next=/");
  }

  const { user } = session;

  return (
    <>
      <TopNav name={user.name} email={user.email} />
      <main className={styles.content}>
        <h1 className={styles.heading}>Welcome, {user.name.split(" ")[0]}</h1>
        <p className={styles.subtle}>Your workspaces will show up here.</p>
        <section className={styles.panel}>
          <h2 className={styles.panelTitle}>Account</h2>
          <dl className={styles.list}>
            <dt>Name</dt>
            <dd>{user.name}</dd>
            <dt>Email</dt>
            <dd>{user.email}</dd>
            <dt>Role</dt>
            <dd>{user.role}</dd>
          </dl>
        </section>
      </main>
    </>
  );
}
