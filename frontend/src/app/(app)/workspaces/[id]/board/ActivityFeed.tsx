"use client";

import { Bot } from "lucide-react";
import { useEffect, useState } from "react";
import { useI18n } from "@/i18n/client";
import { sendJson } from "@/lib/client";
import type { ActivityEntry, SubtaskStatus } from "@/lib/board-types";
import styles from "./activity.module.css";

const UPDATED_FIELDS = ["title", "description", "priority", "points", "labels", "criteria"] as const;
type UpdatedField = (typeof UPDATED_FIELDS)[number];

type ActivityFeedProps = {
  workspaceId: string;
  /** "stories/PRU-1" for a story and its subtasks, or "subtasks/PRU-2" for one subtask. */
  scope: string;
  /** Changes whenever something that writes history happened, so the list is loaded again. */
  version: string;
};

export function ActivityFeed({ workspaceId, scope, version }: ActivityFeedProps) {
  const { t, locale } = useI18n();
  const [entries, setEntries] = useState<ActivityEntry[] | null>(null);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    let active = true;

    void sendJson<ActivityEntry[]>(`/api/workspaces/${workspaceId}/${scope}/activity`, "GET").then((result) => {
      if (!active) return;
      setFailed(!result.ok);
      if (result.ok && result.data) setEntries(result.data);
    });

    return () => {
      active = false;
    };
  }, [workspaceId, scope, version]);

  function status(value: string | null) {
    return value ? t(`story.subtaskState.${value as SubtaskStatus}`) : "";
  }

  function describe(entry: ActivityEntry) {
    switch (entry.type) {
      case "STORY_CREATED":
        return t("activity.STORY_CREATED");
      case "STORY_MOVED":
        return t("activity.STORY_MOVED", { from: entry.from ?? "", to: entry.to ?? "" });
      case "STORY_UPDATED":
        return t("activity.STORY_UPDATED", {
          fields: (entry.to ?? "")
            .split(",")
            .filter((field): field is UpdatedField => (UPDATED_FIELDS as readonly string[]).includes(field))
            .map((field) => t(`activity.field.${field}`))
            .join(", "),
        });
      case "STORY_POINTS_CHANGED":
        if (!entry.to) return t("activity.POINTS_CLEARED");
        if (!entry.from) return t("activity.POINTS_SET", { to: entry.to });
        return t("activity.POINTS_CHANGED", { from: entry.from, to: entry.to });
      case "STORY_ASSIGNED":
        if (!entry.to) return t("activity.ASSIGNEE_CLEARED");
        if (!entry.from) return t("activity.ASSIGNEE_SET", { to: entry.to });
        return t("activity.ASSIGNEE_CHANGED", { from: entry.from, to: entry.to });
      case "SUBTASK_ADDED":
        return t("activity.SUBTASK_ADDED", { key: entry.subtaskKey ?? "", title: entry.to ?? "" });
      case "SUBTASK_STATUS_CHANGED":
        return t("activity.SUBTASK_STATUS_CHANGED", {
          key: entry.subtaskKey ?? "",
          from: status(entry.from),
          to: status(entry.to),
        });
    }
  }

  function when(value: string) {
    return new Date(value).toLocaleString(locale, { dateStyle: "short", timeStyle: "short" });
  }

  return (
    <section className={styles.section}>
      <h3 className={styles.title}>{t("activity.title")}</h3>
      {failed && entries === null ? <p className={styles.hint}>{t("activity.failed")}</p> : null}
      {entries !== null && entries.length === 0 ? <p className={styles.hint}>{t("activity.empty")}</p> : null}
      <ol className={styles.list}>
        {(entries ?? []).map((entry) => (
          <li key={entry.id} className={styles.item} data-source={entry.source}>
            <span className={styles.text}>
              <strong className={styles.actor}>
                {entry.source === "GITHUB" ? (
                  <>
                    <Bot size={14} aria-hidden="true" /> {t("activity.github")}
                  </>
                ) : (
                  (entry.actorName ?? t("activity.someone"))
                )}
              </strong>{" "}
              {describe(entry)}
            </span>
            <time className={styles.time} dateTime={entry.createdAt}>
              {when(entry.createdAt)}
            </time>
          </li>
        ))}
      </ol>
    </section>
  );
}
