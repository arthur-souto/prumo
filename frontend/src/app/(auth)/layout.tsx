import type { ReactNode } from "react";
import { Logo } from "@/components/ui/Logo";
import styles from "./auth.module.css";

export default function AuthLayout({ children }: { children: ReactNode }) {
  return (
    <main className={styles.page}>
      <div className={styles.card}>
        <div className={styles.brand}>
          <Logo size="lg" />
        </div>
        {children}
      </div>
      <p className={styles.footer}>Prumo is open source under the MIT license.</p>
    </main>
  );
}
