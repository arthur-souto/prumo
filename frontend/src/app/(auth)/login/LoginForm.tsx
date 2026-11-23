"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, useT } from "@/i18n/client";
import { sendJson } from "@/lib/client";
import styles from "../AuthForm.module.css";

export function LoginForm({ oauthError }: { oauthError?: string }) {
  const router = useRouter();
  const t = useT();
  const [error, setError] = useState<string | undefined>(oauthError);
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(undefined);

    const result = await sendJson("/api/auth/login", "POST", {
      email: data.get("email"),
      password: data.get("password"),
    });

    if (result.ok) {
      router.replace("/");
      router.refresh();
      return;
    }

    setError(errorMessage(t, result.code));
    setSubmitting(false);
  }

  return (
    <>
      {error ? (
        <div className={styles.alert}>
          <Alert>{error}</Alert>
        </div>
      ) : null}
      <form className={styles.form} onSubmit={onSubmit}>
        <TextField
          label={t("auth.login.email")}
          name="email"
          type="email"
          autoComplete="email"
          placeholder={t("auth.login.emailPlaceholder")}
          required
          autoFocus
        />
        <TextField
          label={t("auth.login.password")}
          name="password"
          type="password"
          autoComplete="current-password"
          placeholder={t("auth.login.passwordPlaceholder")}
          required
        />
        <Button type="submit" appearance="primary" fullWidth loading={submitting}>
          {t("auth.login.submit")}
        </Button>
      </form>
    </>
  );
}
