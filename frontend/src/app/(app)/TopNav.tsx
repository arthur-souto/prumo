"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { Logo } from "@/components/ui/Logo";
import styles from "./TopNav.module.css";

type TopNavProps = {
  name: string;
  email: string;
};

export function TopNav({ name, email }: TopNavProps) {
  const router = useRouter();
  const [leaving, setLeaving] = useState(false);

  async function logout() {
    setLeaving(true);
    await fetch("/api/auth/logout", { method: "POST" }).catch(() => null);
    router.replace("/login");
    router.refresh();
  }

  return (
    <header className={styles.nav}>
      <Logo />
      <div className={styles.account}>
        <div className={styles.identity}>
          <span className={styles.name}>{name}</span>
          <span className={styles.email}>{email}</span>
        </div>
        <span className={styles.avatar} aria-hidden="true">
          {name.charAt(0).toUpperCase()}
        </span>
        <Button appearance="subtle" onClick={logout} loading={leaving}>
          Log out
        </Button>
      </div>
    </header>
  );
}
