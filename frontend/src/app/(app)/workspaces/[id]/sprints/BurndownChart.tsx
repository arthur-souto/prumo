"use client";

import { useEffect, useMemo, useRef, useState, type PointerEvent } from "react";
import { Skeleton } from "@/components/ui/Skeleton";
import { useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import type { BurndownDay, Sprint, SprintBurndown } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { localToday } from "./sprint-metrics";
import styles from "./sprint-charts.module.css";

type BurndownChartProps = {
  workspaceId: string;
  sprint: Sprint;
  refreshKey: string;
};

type Load = { key: string; data: SprintBurndown | null; failed: boolean };

const HEIGHT = 240;
const PAD = { top: 16, right: 56, bottom: 28, left: 36 };

function ticksFor(max: number) {
  const raw = Math.max(1, max / 4);
  const step = [1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500].find((candidate) => candidate >= raw) ?? Math.ceil(raw);
  const top = Math.ceil(max / step) * step;
  const ticks = [];
  for (let value = 0; value <= top; value += step) ticks.push(value);
  return { ticks, top };
}

function stepPath(points: { x: number; y: number }[]) {
  return points
    .map((point, index) => (index === 0 ? `M${point.x},${point.y}` : `H${point.x}V${point.y}`))
    .join("");
}

export function BurndownChart({ workspaceId, sprint, refreshKey }: BurndownChartProps) {
  const { t, locale } = useI18n();
  const key = `${sprint.id}:${refreshKey}`;
  const [load, setLoad] = useState<Load | null>(null);
  const [width, setWidth] = useState(0);
  const [hover, setHover] = useState<number | null>(null);
  const plotRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let current = true;
    sendJson<SprintBurndown>(`/api/workspaces/${workspaceId}/sprints/${sprint.id}/metrics`, "GET").then((response) => {
      if (!current) return;
      setLoad({ key, data: response.ok && response.data ? response.data : null, failed: !response.ok });
    });

    return () => {
      current = false;
    };
  }, [workspaceId, sprint.id, key]);

  useEffect(() => {
    const node = plotRef.current;
    if (!node) return;
    const observer = new ResizeObserver(([entry]) => setWidth(Math.floor(entry.contentRect.width)));
    observer.observe(node);
    return () => observer.disconnect();
  }, [load?.data]);

  const days = useMemo(() => load?.data?.burndown ?? [], [load?.data]);
  const dayLabel = (iso: string) => new Date(`${iso}T00:00:00Z`).toLocaleDateString(dateLocale(locale), { day: "numeric", month: "short", timeZone: "UTC" });
  const fullDate = (iso: string) =>
    new Date(`${iso}T00:00:00Z`).toLocaleDateString(dateLocale(locale), { weekday: "short", day: "numeric", month: "short", timeZone: "UTC" });

  const geometry = useMemo(() => {
    if (days.length === 0 || width === 0) return null;

    const highest = Math.max(1, ...days.map((day) => Math.max(day.totalPoints ?? 0, day.remainingPoints ?? 0, day.idealPoints)));
    const { ticks, top } = ticksFor(highest);
    const plotWidth = Math.max(1, width - PAD.left - PAD.right);
    const plotHeight = HEIGHT - PAD.top - PAD.bottom;
    const x = (index: number) => PAD.left + (days.length === 1 ? plotWidth / 2 : (index / (days.length - 1)) * plotWidth);
    const y = (value: number) => PAD.top + plotHeight - (value / top) * plotHeight;
    const measured = days.map((day, index) => ({ day, index })).filter(({ day }) => day.remainingPoints !== null);
    const labelEvery = Math.max(1, Math.ceil(days.length / Math.max(2, Math.floor(plotWidth / 56))));

    return { ticks, x, y, plotWidth, plotHeight, measured, labelEvery };
  }, [days, width]);

  if (!load || load.key !== key) {
    return (
      <figure className={styles.chart}>
        <figcaption className={styles.title}>{t("sprints.burndown.title")}</figcaption>
        <Skeleton height={HEIGHT} />
      </figure>
    );
  }

  if (load.failed || !load.data) {
    return (
      <figure className={styles.chart}>
        <figcaption className={styles.title}>{t("sprints.burndown.title")}</figcaption>
        <p className={styles.chartNote}>{t("sprints.burndown.failed")}</p>
      </figure>
    );
  }

  const today = localToday();
  const todayIndex = sprint.state === "ACTIVE" ? days.findIndex((day) => day.date === today) : -1;
  const last = geometry?.measured.at(-1);
  const hovered = hover === null ? null : days[hover];

  function onMove(event: PointerEvent<SVGSVGElement>) {
    if (!geometry) return;
    const box = event.currentTarget.getBoundingClientRect();
    const position = event.clientX - box.left - PAD.left;
    const index = days.length === 1 ? 0 : Math.round((position / geometry.plotWidth) * (days.length - 1));
    setHover(Math.min(days.length - 1, Math.max(0, index)));
  }

  const value = (day: BurndownDay, field: "remainingPoints" | "totalPoints") =>
    day[field] === null ? "–" : t("sprints.pointsShort", { count: day[field] });

  return (
    <figure className={styles.chart}>
      <div className={styles.titleRow}>
        <figcaption className={styles.title}>{t("sprints.burndown.title")}</figcaption>
        <ul className={styles.legend}>
          <li>
            <span className={styles.lineKey} style={{ background: "var(--series-1)" }} aria-hidden="true" />
            {t("sprints.burndown.remaining")}
          </li>
          <li>
            <span className={styles.lineKey} style={{ background: "var(--series-2)" }} aria-hidden="true" />
            {t("sprints.burndown.scope")}
          </li>
          <li>
            <span className={`${styles.lineKey} ${styles.lineKeyDashed}`} aria-hidden="true" />
            {t("sprints.burndown.ideal")}
          </li>
        </ul>
      </div>

      <div ref={plotRef} className={styles.plot}>
        {geometry ? (
          <svg
            className={styles.burndown}
            width={width}
            height={HEIGHT}
            role="img"
            aria-label={t("sprints.burndown.summary", {
              remaining: last ? (last.day.remainingPoints ?? 0) : 0,
              total: load.data.baselinePoints,
            })}
            onPointerMove={onMove}
            onPointerLeave={() => setHover(null)}
          >
            {geometry.ticks.map((tick) => (
              <g key={tick}>
                <line className={styles.grid} x1={PAD.left} x2={PAD.left + geometry.plotWidth} y1={geometry.y(tick)} y2={geometry.y(tick)} />
                <text className={styles.axis} x={PAD.left - 8} y={geometry.y(tick)} textAnchor="end" dominantBaseline="middle">
                  {tick}
                </text>
              </g>
            ))}

            {days.map((day, index) =>
              index % geometry.labelEvery === 0 || index === days.length - 1 ? (
                <text
                  key={day.date}
                  className={styles.axis}
                  x={geometry.x(index)}
                  y={HEIGHT - 8}
                  textAnchor={index === 0 ? "start" : index === days.length - 1 ? "end" : "middle"}
                >
                  {dayLabel(day.date)}
                </text>
              ) : null,
            )}

            {todayIndex >= 0 ? (
              <g>
                <line className={styles.todayLine} x1={geometry.x(todayIndex)} x2={geometry.x(todayIndex)} y1={PAD.top} y2={PAD.top + geometry.plotHeight} />
                <text className={styles.todayText} x={geometry.x(todayIndex)} y={PAD.top - 4} textAnchor="middle">
                  {t("sprints.burndown.today")}
                </text>
              </g>
            ) : null}

            <polyline
              className={styles.idealLine}
              points={days.map((day, index) => `${geometry.x(index)},${geometry.y(day.idealPoints)}`).join(" ")}
            />

            {geometry.measured.length > 0 ? (
              <>
                <path
                  className={styles.seriesLine}
                  style={{ stroke: "var(--series-2)" }}
                  d={stepPath(geometry.measured.map(({ day, index }) => ({ x: geometry.x(index), y: geometry.y(day.totalPoints ?? 0) })))}
                />
                <path
                  className={styles.seriesLine}
                  style={{ stroke: "var(--series-1)" }}
                  d={stepPath(geometry.measured.map(({ day, index }) => ({ x: geometry.x(index), y: geometry.y(day.remainingPoints ?? 0) })))}
                />
              </>
            ) : null}

            {last ? (
              <text className={styles.endLabel} x={geometry.x(last.index) + 8} y={geometry.y(last.day.remainingPoints ?? 0)} dominantBaseline="middle">
                {t("sprints.pointsShort", { count: last.day.remainingPoints ?? 0 })}
              </text>
            ) : null}

            {hovered && hover !== null ? (
              <g>
                <line className={styles.crosshair} x1={geometry.x(hover)} x2={geometry.x(hover)} y1={PAD.top} y2={PAD.top + geometry.plotHeight} />
                {hovered.totalPoints !== null ? (
                  <circle className={styles.marker} style={{ fill: "var(--series-2)" }} cx={geometry.x(hover)} cy={geometry.y(hovered.totalPoints)} r={4} />
                ) : null}
                {hovered.remainingPoints !== null ? (
                  <circle className={styles.marker} style={{ fill: "var(--series-1)" }} cx={geometry.x(hover)} cy={geometry.y(hovered.remainingPoints)} r={4} />
                ) : null}
              </g>
            ) : null}
          </svg>
        ) : (
          <div style={{ height: HEIGHT }} />
        )}

        {hovered && hover !== null && geometry ? (
          <div
            className={styles.tooltip}
            role="tooltip"
            style={{
              left: Math.min(Math.max(geometry.x(hover), 80), width - 80),
              top: geometry.y(Math.max(hovered.totalPoints ?? 0, hovered.idealPoints)),
            }}
          >
            <strong>{fullDate(hovered.date)}</strong>
            <span>
              {t("sprints.burndown.remaining")}: {value(hovered, "remainingPoints")}
            </span>
            <span>
              {t("sprints.burndown.scope")}: {value(hovered, "totalPoints")}
            </span>
            <span>
              {t("sprints.burndown.ideal")}: {t("sprints.pointsShort", { count: Math.round(hovered.idealPoints * 10) / 10 })}
            </span>
          </div>
        ) : null}
      </div>

      <details className={styles.dataTable}>
        <summary>{t("sprints.burndown.showTable")}</summary>
        <table className={styles.table}>
          <thead>
            <tr>
              <th scope="col">{t("sprints.burndown.day")}</th>
              <th scope="col" className={styles.num}>{t("sprints.burndown.remaining")}</th>
              <th scope="col" className={styles.num}>{t("sprints.burndown.scope")}</th>
              <th scope="col" className={styles.num}>{t("sprints.burndown.ideal")}</th>
            </tr>
          </thead>
          <tbody>
            {days.map((day) => (
              <tr key={day.date} data-empty={day.remainingPoints === null ? "true" : undefined}>
                <th scope="row">{fullDate(day.date)}</th>
                <td className={styles.num}>{value(day, "remainingPoints")}</td>
                <td className={styles.num}>{value(day, "totalPoints")}</td>
                <td className={styles.num}>{Math.round(day.idealPoints * 10) / 10}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </details>
    </figure>
  );
}
