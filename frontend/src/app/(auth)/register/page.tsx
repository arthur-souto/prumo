import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { getSession } from "@/lib/session";
import { GithubButton } from "../GithubButton";
import styles from "../AuthForm.module.css";
import { RegisterForm } from "./RegisterForm";

export const metadata: Metadata = { title: "Sign up" };

export default async function RegisterPage() {
  if ((await getSession()).kind === "authenticated") {
    redirect("/");
  }

  return (
    <>
      <h1 className={styles.title}>Create your account</h1>
      <RegisterForm />
      <div className={styles.divider}>Or</div>
      <GithubButton label="Sign up with GitHub" />
      <p className={styles.switch}>
        Already have an account? <Link href="/login">Log in</Link>
      </p>
    </>
  );
}
