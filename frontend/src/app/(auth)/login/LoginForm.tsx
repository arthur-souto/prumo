"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import styles from "../AuthForm.module.css";

export function LoginForm({ oauthError }: { oauthError?: string }) {
  const router = useRouter();
  const [error, setError] = useState<string | undefined>(oauthError);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(undefined);

    const response = await fetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: data.get("email"), password: data.get("password") }),
    }).catch(() => null);

    if (response?.ok) {
      router.replace("/");
      router.refresh();
      return;
    }

    const body = await response?.json().catch(() => null);
    setError(body?.message ?? "The service is unavailable. Try again shortly.");
    setSubmitting(false);
  }

  return (
    <>
      {error ? (
        <div className={styles.alert}>
          <Alert>{error}</Alert>
        </div>
      ) : null}
      <form className={styles.form} onSubmit={onSubmit} noValidate={false}>
        <TextField label="Email" name="email" type="email" autoComplete="email" placeholder="you@company.com" required autoFocus />
        <TextField label="Password" name="password" type="password" autoComplete="current-password" placeholder="Enter your password" required />
        <Button type="submit" appearance="primary" fullWidth loading={submitting}>
          Log in
        </Button>
      </form>
    </>
  );
}
