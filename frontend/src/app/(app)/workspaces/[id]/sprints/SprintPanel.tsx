"use client";

import { IterationCw, MoreHorizontal, Pencil, Play, Square, Trash2, User, X } from "lucide-react";
import Link from "next/link";
import { useMemo } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { Badge, type BadgeTone } from "@/components/ui/Badge";
import { Button } from "@/components/ui/Button";
import { DropdownContent, DropdownItem, DropdownMenu, DropdownTrigger } from "@/components/ui/DropdownMenu";
import { EmptyState } from "@/components/ui/EmptyState";
import { IconButton } from "@/components/ui/IconButton";
import { useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import type { Column, Member, Sprint, SprintState, Story } from "@/lib/board-types";
import { formatDate } from "@/lib/workspace-types";
import { PriorityBadge } from "../board/PriorityBadge";
import { AssigneeChart, StatusChart } from "./SprintCharts";
import { localToday, percent, sprintMetrics } from "./sprint-metrics";
import charts from "./sprint-charts.module.css";
import styles from "./sprints.module.css";

export const STATE_TONE: Record<SprintState, BadgeTone> = { ACTIVE: "primary", PLANNED: "neutral", CLOSED: "success" };

type SprintPanelProps = {
  workspaceId: string;
  sprint: Sprint;
  stories: Story[];
  columns: Column[];
  members: Member[];
  canManage: boolean;
  canStart: boolean;
  starting: boolean;
  onStart: () => void;
  onClose: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onDismiss: () => void;
};

type Fact = { label: string; value: string };

export function SprintPanel({
  workspaceId,
  sprint,
  stories,
  columns,
  members,
  canManage,
  canStart,
  starting,
  onStart,
  onClose,
  onEdit,
  onDelete,
  onDismiss,
}: SprintPanelProps) {
  const { t, locale } = useI18n();
  const metrics = useMemo(() => sprintMetrics(sprint, stories, columns, members, localToday()), [sprint, stories, columns, members]);
  const { days } = metrics;
  const date = (value: string) => formatDate(value, dateLocale(locale));
  const slotOf = useMemo(() => new Map(metrics.byStatus.map((slice) => [slice.id, slice.slot])), [metrics.byStatus]);
  const orderedColumns = useMemo(() => [...columns].sort((a, b) => a.position - b.position), [columns]);

  const remaining = metrics.totalPoints - metrics.donePoints;
  const timeShare = percent(days.elapsed, days.total);
  const workShare = percent(metrics.donePoints, metrics.totalPoints);
  const pace = Math.round(metrics.donePoints - (metrics.totalPoints * days.elapsed) / Math.max(1, days.total));

  const hero =
    sprint.state === "CLOSED"
      ? { value: String(sprint.completedPoints ?? 0), of: t("sprints.heroOfCommitted", { count: sprint.committedPoints ?? 0 }) }
      : sprint.state === "ACTIVE"
        ? { value: String(metrics.donePoints), of: t("sprints.heroOfTotal", { count: metrics.totalPoints }) }
        : { value: String(metrics.totalPoints), of: t("sprints.heroPlanned") };

  const facts: Fact[] =
    sprint.state === "CLOSED"
      ? [
          { label: t("sprints.deliveryStat"), value: `${percent(sprint.completedPoints ?? 0, sprint.committedPoints ?? 0)}%` },
          { label: t("sprints.storiesStat"), value: String(metrics.stories.length) },
          { label: t("sprints.durationStat"), value: t("sprints.daysTotal", { count: days.total }) },
        ]
      : sprint.state === "ACTIVE"
        ? [
            { label: t("sprints.remainingStat"), value: t("sprints.pointsShort", { count: remaining }) },
            { label: t("sprints.storiesStat"), value: t("sprints.storiesOf", { done: metrics.doneStories, total: metrics.stories.length }) },
            { label: t("sprints.daysLeftStat"), value: t("sprints.daysLeftOf", { left: days.remaining, total: days.total }) },
            { label: t("sprints.committedStat"), value: t("sprints.pointsShort", { count: sprint.committedPoints ?? 0 }) },
          ]
        : [
            { label: t("sprints.storiesStat"), value: String(metrics.stories.length) },
            { label: t("sprints.startsInStat"), value: t("sprints.daysTotal", { count: days.untilStart }) },
            { label: t("sprints.durationStat"), value: t("sprints.daysTotal", { count: days.total }) },
          ];

  return (
    <section className={`${styles.panel} ${charts.palette}`} aria-label={sprint.name}>
      <header className={styles.panelHead}>
        <div className={styles.panelTitle}>
          <div className={styles.titleLine}>
            <h2 className={styles.panelName}>{sprint.name}</h2>
            <Badge tone={STATE_TONE[sprint.state]}>{t(`sprints.states.${sprint.state}`)}</Badge>
          </div>
          <p className={styles.panelMeta}>
            {t("sprints.dates", { start: date(sprint.startDate), end: date(sprint.endDate) })}
            <span aria-hidden="true"> · </span>
            {t("sprints.daysTotal", { count: days.total })}
          </p>
          {sprint.goal ? <p className={styles.goal}>{sprint.goal}</p> : null}
        </div>
        <div className={styles.panelActions}>
          {canManage && sprint.state !== "CLOSED" ? (
            <>
            {sprint.state === "ACTIVE" ? (
              <Button size="sm" icon={<Square size={14} aria-hidden="true" />} onClick={onClose}>
                {t("sprints.close")}
              </Button>
            ) : (
              <Button size="sm" appearance={canStart ? "primary" : "default"} icon={<Play size={14} aria-hidden="true" />} loading={starting} onClick={onStart}>
                {t("sprints.start")}
              </Button>
            )}
            <DropdownMenu>
              <DropdownTrigger>
                <IconButton size="sm" label={t("story.moreActions")} icon={<MoreHorizontal size={16} aria-hidden="true" />} />
              </DropdownTrigger>
              <DropdownContent>
                <DropdownItem icon={<Pencil size={16} aria-hidden="true" />} onSelect={onEdit}>
                  {t("sprints.edit")}
                </DropdownItem>
                {sprint.state === "PLANNED" ? (
                  <DropdownItem icon={<Trash2 size={16} aria-hidden="true" />} danger onSelect={onDelete}>
                    {t("sprints.delete")}
                  </DropdownItem>
                ) : null}
              </DropdownContent>
            </DropdownMenu>
            </>
          ) : null}
          <IconButton size="sm" label={t("common.close")} icon={<X size={18} aria-hidden="true" />} onClick={onDismiss} />
        </div>
      </header>

      <div className={styles.summary}>
        <div className={styles.hero}>
          <span className={styles.heroValue}>{hero.value}</span>
          <span className={styles.heroOf}>{hero.of}</span>
        </div>
        <dl className={styles.facts}>
          {facts.map((fact) => (
            <div key={fact.label} className={styles.fact}>
              <dt>{fact.label}</dt>
              <dd>{fact.value}</dd>
            </div>
          ))}
        </dl>

        {sprint.state === "ACTIVE" ? (
          <div className={styles.progress}>
            <div
              className={styles.progressTrack}
              role="progressbar"
              aria-label={t("sprints.workDone")}
              aria-valuenow={workShare}
              aria-valuemin={0}
              aria-valuemax={100}
            >
              <span className={styles.progressFill} style={{ width: `${workShare}%` }} />
              <span className={styles.today} style={{ left: `${timeShare}%` }} aria-hidden="true" />
            </div>
            <div className={styles.progressLegend}>
              <span>{t("sprints.workShare", { share: workShare })}</span>
              <span className={styles.todayLabel}>{t("sprints.todayMarker", { elapsed: days.elapsed, total: days.total })}</span>
              {metrics.totalPoints > 0 ? (
                <span className={styles.pace} data-tone={pace >= 0 ? "ahead" : "behind"}>
                  {pace === 0
                    ? t("sprints.onPace")
                    : pace > 0
                      ? t("sprints.ahead", { count: pace })
                      : t("sprints.behind", { count: -pace })}
                </span>
              ) : null}
            </div>
          </div>
        ) : null}
      </div>

      {metrics.stories.length === 0 ? (
        <EmptyState
          compact
          icon={<IterationCw size={24} aria-hidden="true" />}
          title={t("sprints.noStoriesTitle")}
          description={sprint.state === "CLOSED" ? t("sprints.noStoriesClosed") : t("sprints.noStoriesText")}
        />
      ) : (
        <div className={styles.body}>
          <div className={styles.main}>
            <h3 className={styles.subTitle}>
              {t("sprints.storiesStat")} <span className={styles.subCount}>{metrics.stories.length}</span>
            </h3>
            <div className={styles.storyTable}>
              {orderedColumns.map((column) => {
                const inColumn = metrics.stories.filter((story) => story.columnId === column.id);
                if (inColumn.length === 0) return null;

                return (
                  <div key={column.id} className={styles.group}>
                    <div className={styles.groupHead}>
                      <span className={styles.dot} style={{ background: `var(--series-${slotOf.get(column.id) ?? 8})` }} aria-hidden="true" />
                      <span>{column.name}</span>
                      <span className={styles.groupCount}>{inColumn.length}</span>
                    </div>
                    <ul className={styles.rows}>
                      {inColumn.map((story) => {
                        const assignee = members.find((member) => member.id === story.assigneeId);

                        return (
                          <li key={story.id}>
                            <Link
                              className={styles.storyRow}
                              data-done={column.kind === "DONE" ? "true" : undefined}
                              href={`/workspaces/${workspaceId}/board?story=${encodeURIComponent(story.key)}`}
                            >
                              <span className={styles.storyKey}>{story.key}</span>
                              <span className={styles.storyTitle}>{story.title}</span>
                              <PriorityBadge priority={story.priority} label={t(`priority.${story.priority}`)} />
                              <span className={styles.storyPoints}>{story.storyPoints ?? "–"}</span>
                              {assignee ? (
                                <Avatar name={assignee.name} seed={assignee.id} src={assignee.avatarUrl} size="sm" />
                              ) : (
                                <span className={styles.nobody} title={t("story.unassigned")}>
                                  <User size={12} aria-hidden="true" />
                                </span>
                              )}
                            </Link>
                          </li>
                        );
                      })}
                    </ul>
                  </div>
                );
              })}
            </div>
          </div>

          <aside className={styles.aside}>
            {metrics.totalPoints === 0 ? (
              <p className={styles.noPoints}>
                <strong>{t("sprints.noPointsTitle")}</strong> {t("sprints.noPointsText")}
              </p>
            ) : (
              <>
                <StatusChart slices={metrics.byStatus} total={metrics.totalPoints} />
                <AssigneeChart rows={metrics.byAssignee} />
              </>
            )}
          </aside>
        </div>
      )}
    </section>
  );
}
