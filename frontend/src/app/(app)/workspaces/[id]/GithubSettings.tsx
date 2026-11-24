"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { Alert } from "@/components/ui/Alert";
import { Button } from "@/components/ui/Button";
import { Select } from "@/components/ui/Select";
import { errorMessage, githubReason, useT } from "@/i18n/client";
import type { GithubConnection, Repository } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { dateLocale } from "@/i18n/core";
import { useI18n } from "@/i18n/client";
import styles from "./workspace.module.css";

type GithubSettingsProps = {
  workspaceId: string;
  canEdit: boolean;
  connection: GithubConnection;
  repositories: Repository[];
};

export function GithubSettings({ workspaceId, canEdit, connection, repositories }: GithubSettingsProps) {
  const router = useRouter();
  const t = useT();
  const { locale } = useI18n();
  const [error, setError] = useState<string>();
  const [busy, setBusy] = useState(false);
  const [accessible, setAccessible] = useState<Repository[]>([]);
  const [chosen, setChosen] = useState("");

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

  async function loadRepositories() {
    setBusy(true);
    setError(undefined);
    const result = await sendJson<Repository[]>("/api/github/repositories", "GET");
    setBusy(false);

    if (!result.ok || !result.data) {
      setError(errorMessage(t, result.code, result.reason));
      return;
    }

    const connected = new Set(repositories.map((repository) => repository.fullName.toLowerCase()));
    setAccessible(result.data.filter((repository) => !connected.has(repository.fullName.toLowerCase())));
    setChosen("");
  }

  async function connect() {
    const [owner, name] = chosen.split("/");

    if (await run(() => sendJson(`/api/workspaces/${workspaceId}/repositories`, "POST", { owner, name }))) {
      setAccessible((current) => current.filter((repository) => repository.fullName !== chosen));
      setChosen("");
    }
  }

  return (
    <section className={styles.card}>
      <h2 className={styles.cardTitle}>{t("githubSettings.title")}</h2>
      <div className={styles.stack}>
        <p className={styles.hint}>{t("githubSettings.lead")}</p>
        {error ? <Alert>{error}</Alert> : null}

        <p className={styles.hint}>
          {connection.connected
            ? t("githubSettings.connectedAs", { login: connection.login ?? "" })
            : t("githubSettings.notConnected")}{" "}
          <Link href="/settings">{t("githubSettings.manageToken")}</Link>
        </p>

        <h3 className={styles.cardTitle}>{t("githubSettings.repositories")}</h3>
        <p className={styles.hint}>{t("githubSettings.repositoriesLead")}</p>

        {repositories.length === 0 ? (
          <p className={styles.hint}>{t("githubSettings.noRepositories")}</p>
        ) : (
          <ul className={styles.list2}>
            {repositories.map((repository) => (
              <li key={repository.id} className={styles.row}>
                <span className={styles.rowMain}>
                  <span className={styles.rowTitle}>{repository.fullName}</span>
                  <span className={styles.rowMeta}>
                    {repository.lastSyncedAt
                      ? t("githubSettings.lastSynced", {
                          time: new Date(repository.lastSyncedAt).toLocaleString(dateLocale(locale)),
                        })
                      : t("githubSettings.neverSynced")}
                  </span>
                  {repository.lastSyncError ? (
                    <span className={styles.rowError}>
                      {t("githubSettings.syncFailed", { reason: githubReason(t, repository.lastSyncError) })}
                    </span>
                  ) : null}
                </span>
                {canEdit ? (
                  <Button
                    appearance="subtle"
                    disabled={busy}
                    onClick={() =>
                      run(() => sendJson(`/api/workspaces/${workspaceId}/repositories/${repository.id}`, "DELETE"))
                    }
                  >
                    {t("githubSettings.disconnect")}
                  </Button>
                ) : null}
              </li>
            ))}
          </ul>
        )}

        {canEdit && connection.connected ? (
          accessible.length === 0 ? (
            <Button loading={busy} onClick={loadRepositories}>
              {t("githubSettings.choose")}
            </Button>
          ) : (
            <div className={styles.inline}>
              <Select
                value={chosen}
                aria-label={t("githubSettings.choose")}
                onChange={(event) => setChosen(event.target.value)}
              >
                <option value="">{t("githubSettings.choose")}</option>
                {accessible.map((repository) => (
                  <option key={repository.fullName} value={repository.fullName}>
                    {repository.fullName}
                  </option>
                ))}
              </Select>
              <Button appearance="primary" disabled={chosen === ""} loading={busy} onClick={connect}>
                {t("githubSettings.connect")}
              </Button>
            </div>
          )
        ) : null}
      </div>
    </section>
  );
}
