"use client";

import { useRouter } from "next/navigation";
import { useState, type FormEvent } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { TextField } from "@/components/ui/TextField";
import { errorMessage, useT } from "@/i18n/client";
import type { GithubConnection } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import styles from "../workspaces/[id]/workspace.module.css";

export function GithubTokenSettings({ connection }: { connection: GithubConnection }) {
  const router = useRouter();
  const t = useT();
  const [token, setToken] = useState("");
  const [error, setError] = useState<string>();
  const [busy, setBusy] = useState(false);

  async function run(action: () => Promise<{ ok: boolean; code: string; reason?: string }>) {
    setBusy(true);
    setError(undefined);
    const result = await action();
    setBusy(false);

    if (!result.ok) {
      setError(errorMessage(t, result.code, result.reason));
      return false;
    }

    router.refresh();
    return true;
  }

  async function save(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    if (await run(() => sendJson("/api/github/token", "PUT", { token }))) {
      setToken("");
    }
  }

  return (
    <section className={styles.card}>
      <h2 className={styles.cardTitle}>{t("githubToken.title")}</h2>
      <div className={styles.stack}>
        <p className={styles.hint}>{t("githubToken.lead")}</p>
        {error ? <Alert>{error}</Alert> : null}

        {connection.connected ? (
          <div className={styles.row}>
            <span className={styles.rowTitle}>{t("githubSettings.connectedAs", { login: connection.login ?? "" })}</span>
            <Button appearance="subtle" loading={busy} onClick={() => run(() => sendJson("/api/github/token", "DELETE"))}>
              {t("githubSettings.remove")}
            </Button>
          </div>
        ) : (
          <p className={styles.hint}>{t("githubSettings.notConnected")}</p>
        )}

        <form className={styles.inline} onSubmit={save}>
          <TextField
            label={t("githubSettings.token")}
            type="password"
            autoComplete="off"
            value={token}
            maxLength={255}
            onChange={(event) => setToken(event.target.value)}
            required
          />
          <Button type="submit" appearance="primary" loading={busy}>
            {t("githubSettings.save")}
          </Button>
        </form>
        <p className={`${styles.hint} ${styles.afterField}`}>{t("githubSettings.tokenHint")}</p>
      </div>
    </section>
  );
}
