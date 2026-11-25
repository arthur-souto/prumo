"use client";

import { IterationCw } from "lucide-react";
import Link from "next/link";
import { useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import type { Sprint } from "@/lib/board-types";
import { formatDate } from "@/lib/workspace-types";
import { localToday, sprintDays } from "../sprints/sprint-metrics";
import styles from "./board.module.css";

export function ActiveSprintBar({ workspaceId, sprint }: { workspaceId: string; sprint: Sprint }) {
  const { t, locale } = useI18n();
  const days = sprintDays(sprint, localToday());
  const date = (value: string) => formatDate(value, dateLocale(locale));

  return (
    <div className={styles.sprintBar}>
      <IterationCw size={16} aria-hidden="true" className={styles.sprintBarIcon} />
      <span className={styles.sprintBarName}>{sprint.name}</span>
      <span className={styles.sprintBarMeta}>{t("sprints.dates", { start: date(sprint.startDate), end: date(sprint.endDate) })}</span>
      {sprint.goal ? <span className={styles.sprintBarGoal}>{sprint.goal}</span> : null}
      <span className={styles.sprintBarDays}>{t("sprints.daysLeft", { count: days.remaining })}</span>
      <Link className={styles.sprintBarLink} href={`/workspaces/${workspaceId}/sprints?sprint=${sprint.id}`}>
        {t("sprints.openSprint")}
      </Link>
    </div>
  );
}
