"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { LanguageSwitcher } from "@/components/ui/LanguageSwitcher";
import { Logo } from "@/components/ui/Logo";
import { useT } from "@/i18n/client";
import styles from "./TopNav.module.css";

type TopNavProps = {
  name: string;
  email: string;
};

export function TopNav({ name, email }: TopNavProps) {
  const router = useRouter();
  const t = useT();
  const [leaving, setLeaving] = useState(false);

  async function logout() {
    setLeaving(true);
    await fetch("/api/auth/logout", { method: "POST" }).catch(() => null);
    router.replace("/login");
    router.refresh();
  }

  return (
    <header className={styles.nav}>
      <div className={styles.left}>
        <Link href="/" aria-label={t("nav.home")}>
          <Logo />
        </Link>
        <Link href="/" className={styles.link}>
          {t("nav.workspaces")}
        </Link>
      </div>
      <div className={styles.account}>
        <LanguageSwitcher />
        <div className={styles.identity}>
          <span className={styles.name}>{name}</span>
          <span className={styles.email}>{email}</span>
        </div>
        <span className={styles.avatar} aria-hidden="true">
          {name.charAt(0).toUpperCase()}
        </span>
        <Button appearance="subtle" onClick={logout} loading={leaving}>
          {t("nav.logout")}
        </Button>
      </div>
    </header>
  );
}
