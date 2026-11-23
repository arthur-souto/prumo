import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getT } from "@/i18n/server";
import type { MessageKey } from "@/i18n/en";
import { getSession } from "@/lib/session";
import { GithubButton } from "../GithubButton";
import styles from "../AuthForm.module.css";
import { LoginForm } from "./LoginForm";

export async function generateMetadata(): Promise<Metadata> {
  const { t } = await getT();
  return { title: t("meta.login") };
}

const OAUTH_ERRORS = [
  "email_already_registered",
  "email_unavailable",
  "account_disabled",
  "oauth_failed",
  "unsupported_provider",
];

type LoginPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  if ((await getSession()).kind === "authenticated") {
    redirect("/");
  }

  const { t } = await getT();
  const { error } = await searchParams;
  const oauthError = error ? t(`auth.oauth.${OAUTH_ERRORS.includes(error) ? error : "oauth_failed"}` as MessageKey) : undefined;

  return (
    <>
      <h1 className={styles.title}>{t("auth.login.title")}</h1>
      <LoginForm oauthError={oauthError} />
      <div className={styles.divider}>{t("auth.or")}</div>
      <GithubButton label={t("auth.login.github")} />
      <p className={styles.switch}>
        {t("auth.login.newHere")} <Link href="/register">{t("auth.login.createAccount")}</Link>
      </p>
    </>
  );
}
