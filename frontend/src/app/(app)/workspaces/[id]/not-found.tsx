import Link from "next/link";
import { getT } from "@/i18n/server";
import styles from "./workspace.module.css";

export default async function WorkspaceNotFound() {
  const { t } = await getT();

  return (
    <main className={styles.page}>
      <section className={styles.missing}>
        <h1 className={styles.title}>{t("workspace.notFoundTitle")}</h1>
        <p className={styles.muted}>{t("workspace.notFoundText")}</p>
        <Link href="/">{t("workspace.back")}</Link>
      </section>
    </main>
  );
}
