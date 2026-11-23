import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { GithubButton } from "../GithubButton";
import styles from "../AuthForm.module.css";
import { LoginForm } from "./LoginForm";

export const metadata: Metadata = { title: "Log in" };

const OAUTH_ERRORS: Record<string, string> = {
  email_already_registered:
    "An account with this email already exists. Log in with your password, then link GitHub from your account settings.",
  email_unavailable: "Your GitHub account has no verified email. Verify one on GitHub and try again.",
  account_disabled: "This account is disabled.",
  oauth_failed: "We could not complete the GitHub login. Try again.",
  unsupported_provider: "This login provider is not supported.",
};

type LoginPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  if ((await getSession()).kind === "authenticated") {
    redirect("/");
  }

  const { error } = await searchParams;
  const oauthError = error ? (OAUTH_ERRORS[error] ?? OAUTH_ERRORS.oauth_failed) : undefined;

  return (
    <>
      <h1 className={styles.title}>Log in to continue</h1>
      <LoginForm oauthError={oauthError} />
      <div className={styles.divider}>Or</div>
      <GithubButton label="Continue with GitHub" />
      <p className={styles.switch}>
        New to Prumo? <Link href="/register">Create an account</Link>
      </p>
    </>
  );
}
