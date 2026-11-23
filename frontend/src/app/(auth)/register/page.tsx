import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getT } from "@/i18n/server";
import { getSession } from "@/lib/session";
import { GithubButton } from "../GithubButton";
import styles from "../AuthForm.module.css";
import { RegisterForm } from "./RegisterForm";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("meta.register") };
}

export default async function RegisterPage() {
  if ((await getSession()).kind === "authenticated") {
    redirect("/");
  }

  const { t } = await getT();

  return (
    <>
      <h1 className={styles.title}>{t("auth.register.title")}</h1>
      <RegisterForm />
      <div className={styles.divider}>{t("auth.or")}</div>
      <GithubButton label={t("auth.register.github")} />
      <p className={styles.switch}>
        {t("auth.register.haveAccount")} <Link href="/login">{t("auth.register.login")}</Link>
      </p>
    </>
  );
}
