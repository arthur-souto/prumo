"use client";

import { useDroppable } from "@dnd-kit/core";
import { ChevronDown, ChevronRight } from "lucide-react";
import { useState, type ReactNode } from "react";
import { useT } from "@/i18n/client";
import type { Column, Member, Sprint, Story } from "@/lib/board-types";
import { IssueRow } from "./IssueRow";
import { PointPills } from "./PointPills";
import { pointsByGroup } from "./planning";
import styles from "./planning.module.css";

type PlanSectionProps = {
  id: string;
  title: ReactNode;
  meta?: ReactNode;
  goal?: string | null;
  actions?: ReactNode;
  stories: Story[];
  total: number;
  columns: Map<string, Column>;
  members: Map<string, Member>;
  targets: Sprint[];
  inBacklog: boolean;
  active?: boolean;
  empty: string;
  limit?: number;
  onShowMore?: () => void;
  onOpen: (story: Story) => void;
  onMove: (story: Story, sprintId: string | null) => void;
};

export function PlanSection({
  id,
  title,
  meta,
  goal,
  actions,
  stories,
  total,
  columns,
  members,
  targets,
  inBacklog,
  active = false,
  empty,
  limit,
  onShowMore,
  onOpen,
  onMove,
}: PlanSectionProps) {
  const t = useT();
  const [open, setOpen] = useState(true);
  const { setNodeRef, isOver } = useDroppable({ id });
  const shown = limit === undefined ? stories : stories.slice(0, limit);

  return (
    <section ref={setNodeRef} className={styles.section} data-over={isOver ? "true" : undefined} data-active={active ? "true" : undefined}>
      <header className={styles.sectionHead}>
        <span className={styles.sectionTitle}>
          <button
            type="button"
            className={styles.chevron}
            aria-expanded={open}
            aria-label={open ? t("sprints.collapse") : t("sprints.expand")}
            onClick={() => setOpen((value) => !value)}
          >
            {open ? <ChevronDown size={16} aria-hidden="true" /> : <ChevronRight size={16} aria-hidden="true" />}
          </button>
          <span className={styles.sectionName}>{title}</span>
          {meta ? <span className={styles.sectionMeta}>{meta}</span> : null}
          <span className={styles.sectionCount}>{t("sprints.issueCount", { count: total })}</span>
        </span>
        <span className={styles.sectionActions}>
          <PointPills totals={pointsByGroup(stories, columns)} />
          {actions}
        </span>
      </header>

      {open ? (
        <>
          {goal ? <p className={styles.sectionGoal}>{goal}</p> : null}
          {stories.length === 0 ? (
            <div className={styles.dropHint}>{empty}</div>
          ) : (
            <ul className={styles.rows}>
              {shown.map((story) => (
                <IssueRow
                  key={story.id}
                  story={story}
                  column={columns.get(story.columnId)}
                  assignee={story.assigneeId ? members.get(story.assigneeId) : undefined}
                  targets={targets.filter((sprint) => sprint.id !== story.sprintId)}
                  inBacklog={inBacklog}
                  onOpen={onOpen}
                  onMove={onMove}
                />
              ))}
            </ul>
          )}
          {limit !== undefined && stories.length > limit ? (
            <button type="button" className={styles.showMore} onClick={onShowMore}>
              {t("sprints.showMore", { count: stories.length - limit })}
            </button>
          ) : null}
        </>
      ) : null}
    </section>
  );
}
