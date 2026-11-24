import { Bot } from "lucide-react";
import { useT } from "@/i18n/client";
import styles from "./origin.module.css";

/** Marks a status or column that the GitHub pull request sync set, as opposed to a person on the board. */
export function GithubOrigin({ compact = false, action }: { compact?: boolean; action: "status" | "column" }) {
  const t = useT();
  const label = t(action === "status" ? "origin.statusByGithub" : "origin.columnByGithub");

  return (
    <span className={compact ? styles.icon : styles.badge} title={label} {...(compact ? { role: "img", "aria-label": label } : {})}>
      <Bot size={compact ? 14 : 12} aria-hidden="true" />
      {compact ? null : <span>{t("origin.viaGithub")}</span>}
    </span>
  );
}
