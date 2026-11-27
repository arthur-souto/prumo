import type { ReactNode } from "react";
import { LanguageSwitcher } from "@/components/ui/LanguageSwitcher";
import { Logo } from "@/components/ui/Logo";
import { ThemeSwitcher } from "@/components/ui/ThemeSwitcher";
import { getT } from "@/i18n/server";
import styles from "./auth.module.css";

export default async function AuthLayout({ children }: { children: ReactNode }) {
  const { t } = await getT();

  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <Logo size="lg" layout="stacked" />
        </div>
        {children}
      </div>
      <div className={styles.footer}>
        <ThemeSwitcher />
        <LanguageSwitcher />
        <p>{t("auth.footer")}</p>
      </div>
    </main>
  );
}
