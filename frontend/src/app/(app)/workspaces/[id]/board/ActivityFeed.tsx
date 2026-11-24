"use client";

import { Bot } from "lucide-react";
import { useEffect, useState } from "react";
import { Avatar } from "@/components/ui/Avatar";
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

  function time(value: string) {
    return new Date(value).toLocaleTimeString(locale, { timeStyle: "short" });
  }

  function exact(value: string) {
    return new Date(value).toLocaleString(locale, { dateStyle: "medium", timeStyle: "short" });
  }

  /** "today" and "yesterday" in the user's language, or the weekday and date for older days. */
  function dayLabel(day: Date) {
    const start = (date: Date) => new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
    const ago = Math.round((start(new Date()) - start(day)) / 86_400_000);

    if (ago >= 0 && ago <= 1) {
      return new Intl.RelativeTimeFormat(locale, { numeric: "auto" }).format(-ago, "day");
    }

    return day.toLocaleDateString(locale, { weekday: "long", day: "numeric", month: "long" });
  }

  const days = new Map<string, { label: string; entries: ActivityEntry[] }>();

  for (const entry of entries ?? []) {
    const day = new Date(entry.createdAt);
    const key = `${day.getFullYear()}-${day.getMonth()}-${day.getDate()}`;
    const group = days.get(key) ?? { label: dayLabel(day), entries: [] };
    group.entries.push(entry);
    days.set(key, group);
  }

  return (
    <section className={styles.section}>
      <h3 className={styles.title}>{t("activity.title")}</h3>
      {failed && entries === null ? <p className={styles.hint}>{t("activity.failed")}</p> : null}
      {entries !== null && entries.length === 0 ? <p className={styles.hint}>{t("activity.empty")}</p> : null}
      <div className={styles.timeline}>
        {[...days.entries()].map(([key, day]) => (
          <div key={key} className={styles.day}>
            <h4 className={styles.dayLabel}>{day.label}</h4>
            <ol className={styles.list}>
              {day.entries.map((entry) => (
                <li key={entry.id} className={styles.item} data-source={entry.source}>
                  <span className={styles.marker}>
                    {entry.source === "GITHUB" ? (
                      <span className={styles.bot} role="img" aria-label={t("activity.github")}>
                        <Bot size={14} aria-hidden="true" />
                      </span>
                    ) : (
                      <Avatar name={entry.actorName ?? t("activity.someone")} seed={entry.actorId ?? undefined} size="sm" />
                    )}
                  </span>
                  <span className={styles.text}>
                    <strong className={styles.actor}>
                      {entry.source === "GITHUB" ? t("activity.github") : (entry.actorName ?? t("activity.someone"))}
                    </strong>{" "}
                    {describe(entry)}
                  </span>
                  <time className={styles.time} dateTime={entry.createdAt} title={exact(entry.createdAt)}>
                    {time(entry.createdAt)}
                  </time>
                </li>
              ))}
            </ol>
          </div>
        ))}
      </div>
    </section>
  );
}
