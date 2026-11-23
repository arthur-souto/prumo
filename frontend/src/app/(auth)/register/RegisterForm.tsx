"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import styles from "../AuthForm.module.css";

export function RegisterForm() {
  const router = useRouter();
  const [error, setError] = useState<string>();
  const [fields, setFields] = useState<Record<string, string>>({});
  const [submitting, setSubmitting] = useState(false);

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    setSubmitting(true);
    setError(undefined);
    setFields({});

    const response = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        name: data.get("name"),
        email: data.get("email"),
        password: data.get("password"),
      }),
    }).catch(() => null);

    const body = await response?.json().catch(() => null);

    if (response?.ok) {
      router.replace(body?.loggedIn ? "/" : "/login");
      router.refresh();
      return;
    }

    setFields(body?.fields ?? {});
    setError(body?.message ?? "The service is unavailable. Try again shortly.");
    setSubmitting(false);
  }

  return (
    <>
      {error && Object.keys(fields).length === 0 ? (
        <div className={styles.alert}>
          <Alert>{error}</Alert>
        </div>
      ) : null}
      <form className={styles.form} onSubmit={onSubmit}>
        <TextField label="Full name" name="name" autoComplete="name" placeholder="Ada Lovelace" maxLength={150} error={fields.name} required autoFocus />
        <TextField label="Work email" name="email" type="email" autoComplete="email" placeholder="you@company.com" error={fields.email} required />
        <TextField
          label="Password"
          name="password"
          type="password"
          autoComplete="new-password"
          placeholder="Create a password"
          hint="Use 8 to 72 characters."
          minLength={8}
          maxLength={72}
          error={fields.password}
          required
        />
        <Button type="submit" appearance="primary" fullWidth loading={submitting}>
          Sign up
        </Button>
      </form>
    </>
  );
}
