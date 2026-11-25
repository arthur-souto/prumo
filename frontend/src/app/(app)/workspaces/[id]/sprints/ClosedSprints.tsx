"use client";

import { ChevronDown, ChevronRight } from "lucide-react";
import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import type { Sprint, SprintPage } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { formatDate } from "@/lib/workspace-types";
import { percent } from "./sprint-metrics";
import styles from "./planning.module.css";

const PAGE_SIZE = 10;

type ClosedSprintsProps = {
  workspaceId: string;
  version: number;
  onOpen: (sprint: Sprint) => void;
};

export function ClosedSprints({ workspaceId, version, onOpen }: ClosedSprintsProps) {
  const { t, locale } = useI18n();
  const [open, setOpen] = useState(false);
  const [pages, setPages] = useState<SprintPage[]>([]);
  const [loading, setLoading] = useState(false);
  const [total, setTotal] = useState<number | null>(null);

  useEffect(() => {
    let current = true;

    sendJson<SprintPage>(`/api/workspaces/${workspaceId}/sprints?state=CLOSED&page=0&size=${PAGE_SIZE}`, "GET").then((response) => {
      if (!current || !response.ok || !response.data) return;
      setPages([response.data]);
      setTotal(response.data.totalItems);
    });

    return () => {
      current = false;
    };
  }, [workspaceId, version]);

  async function loadMore() {
    setLoading(true);
    const next = pages.length;
    const response = await sendJson<SprintPage>(
      `/api/workspaces/${workspaceId}/sprints?state=CLOSED&page=${next}&size=${PAGE_SIZE}`,
      "GET",
    );
    setLoading(false);

    if (response.ok && response.data) setPages((current) => [...current, response.data as SprintPage]);
  }

  if (!total) return null;

  const sprints = pages.flatMap((page) => page.items);
  const date = (value: string) => formatDate(value, dateLocale(locale));

  return (
    <section className={styles.closed}>
      <button type="button" className={styles.toggle} aria-expanded={open} onClick={() => setOpen((value) => !value)}>
        {open ? <ChevronDown size={16} aria-hidden="true" /> : <ChevronRight size={16} aria-hidden="true" />}
        <span className={styles.sectionName}>{t("sprints.closedSprints")}</span>
        <span className={styles.sectionCount}>{total}</span>
      </button>

      {open ? (
        <>
          <ul className={styles.closedRows}>
            {sprints.map((sprint) => (
              <li key={sprint.id}>
                <button type="button" className={styles.closedRow} onClick={() => onOpen(sprint)}>
                  <span className={styles.closedName}>{sprint.name}</span>
                  <span className={styles.closedDates}>{t("sprints.dates", { start: date(sprint.startDate), end: date(sprint.endDate) })}</span>
                  <span className={styles.closedPoints}>
                    {t("sprints.completedOf", { completed: sprint.completedPoints ?? 0, committed: sprint.committedPoints ?? 0 })}
                  </span>
                  <span className={styles.closedShare}>{percent(sprint.completedPoints ?? 0, sprint.committedPoints ?? 0)}%</span>
                  <span className={styles.closedLink}>{t("sprints.report")}</span>
                </button>
              </li>
            ))}
          </ul>
          {sprints.length < total ? (
            <Button size="sm" appearance="subtle" loading={loading} onClick={() => void loadMore()}>
              {t("sprints.loadMore")}
            </Button>
          ) : null}
        </>
      ) : null}
    </section>
  );
}
