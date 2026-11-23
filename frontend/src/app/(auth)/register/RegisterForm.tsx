"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, useT } from "@/i18n/client";
import { sendJson } from "@/lib/client";
import styles from "../AuthForm.module.css";

type RegisterResult = { loggedIn?: boolean };

export function RegisterForm() {
  const router = useRouter();
  const t = useT();
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(undefined);
    setFields({});

    const result = await sendJson<RegisterResult>("/api/auth/register", "POST", {
      name: data.get("name"),
      email: data.get("email"),
      password: data.get("password"),
    });

    if (result.ok) {
      router.replace(result.data?.loggedIn ? "/" : "/login");
      router.refresh();
      return;
    }

    if (result.code === "emailTaken") {
      setFields({ email: errorMessage(t, "emailTaken") });
    } else if (Object.keys(result.fields).length > 0) {
      setFields(result.fields);
    } else {
      setError(errorMessage(t, result.code));
    }

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
          label={t("auth.register.name")}
          name="name"
          autoComplete="name"
          placeholder={t("auth.register.namePlaceholder")}
          maxLength={150}
          error={fields.name}
          required
          autoFocus
        />
        <TextField
          label={t("auth.register.email")}
          name="email"
          type="email"
          autoComplete="email"
          placeholder={t("auth.login.emailPlaceholder")}
          error={fields.email}
          required
        />
        <TextField
          label={t("auth.register.password")}
          name="password"
          type="password"
          autoComplete="new-password"
          placeholder={t("auth.register.passwordPlaceholder")}
          hint={t("auth.register.passwordHint")}
          minLength={8}
          maxLength={72}
          error={fields.password}
          required
        />
        <Button type="submit" appearance="primary" fullWidth loading={submitting}>
          {t("auth.register.submit")}
        </Button>
      </form>
    </>
  );
}
