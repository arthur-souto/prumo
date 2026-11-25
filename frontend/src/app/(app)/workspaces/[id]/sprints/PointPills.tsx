"use client";

import { useT } from "@/i18n/client";
import type { StatusGroup } from "./planning";
import styles from "./planning.module.css";

const GROUPS: StatusGroup[] = ["todo", "doing", "done"];

export function PointPills({ totals }: { totals: Record<StatusGroup, number> }) {
  const t = useT();

  return (
    <span className={styles.pills}>
      {GROUPS.map((group) => (
        <span
          key={group}
          className={styles.pill}
          data-group={group}
          title={t(`sprints.pointsIn.${group}`, { count: totals[group] })}
          aria-label={t(`sprints.pointsIn.${group}`, { count: totals[group] })}
        >
          {totals[group]}
        </span>
      ))}
    </span>
  );
}
