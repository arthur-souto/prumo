"use client";

import { useEffect, useRef, useState } from "react";
import { useI18n } from "@/i18n/client";
import { dateLocale } from "@/i18n/core";
import type { Velocity } from "@/lib/board-types";
import { sendJson } from "@/lib/client";
import { formatDate } from "@/lib/workspace-types";
import { percent } from "./sprint-metrics";
import charts from "./sprint-charts.module.css";
import styles from "./planning.module.css";

type VelocityChartProps = {
  workspaceId: string;
  version: number;
  onOpen: (sprintId: string) => void;
};

const HEIGHT = 220;
const PAD = { top: 16, right: 16, bottom: 28, left: 36 };
const GAP = 2;

function ticksFor(max: number) {
  const raw = Math.max(1, max / 4);
  const step = [1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500].find((candidate) => candidate >= raw) ?? Math.ceil(raw);
  const top = Math.ceil(max / step) * step;
  const ticks = [];
  for (let value = 0; value <= top; value += step) ticks.push(value);
  return { ticks, top: Math.max(top, step) };
}

function bar(x: number, y: number, width: number, height: number) {
  const r = Math.min(4, width / 2, height);
  if (height <= 0) return "";
  return `M${x},${y + height}V${y + r}Q${x},${y} ${x + r},${y}H${x + width - r}Q${x + width},${y} ${x + width},${y + r}V${y + height}Z`;
}

export function VelocityChart({ workspaceId, version, onOpen }: VelocityChartProps) {
  const { t, locale } = useI18n();
  const [data, setData] = useState<Velocity | null>(null);
  const [width, setWidth] = useState(0);
  const [hover, setHover] = useState<number | null>(null);
  const plotRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    let current = true;
    sendJson<Velocity>(`/api/workspaces/${workspaceId}/sprints/velocity`, "GET").then((response) => {
      if (current && response.ok && response.data) setData(response.data);
    });

    return () => {
      current = false;
    };
  }, [workspaceId, version]);

  const hasData = (data?.sprints.length ?? 0) > 0;

  useEffect(() => {
    const node = plotRef.current;
    if (!node) return;
    const observer = new ResizeObserver(([entry]) => setWidth(Math.floor(entry.contentRect.width)));
    observer.observe(node);
    return () => observer.disconnect();
  }, [hasData]);

  if (!data || !hasData) return null;

  const sprints = data.sprints;
  const highest = Math.max(1, ...sprints.map((sprint) => Math.max(sprint.committedPoints ?? 0, sprint.completedPoints ?? 0)));
  const { ticks, top } = ticksFor(highest);
  const plotWidth = Math.max(1, width - PAD.left - PAD.right);
  const plotHeight = HEIGHT - PAD.top - PAD.bottom;
  const slot = plotWidth / sprints.length;
  const BAR = Math.max(8, Math.min(40, slot / 4));
  const y = (value: number) => PAD.top + plotHeight - (value / top) * plotHeight;
  const center = (index: number) => PAD.left + slot * index + slot / 2;
  const maxLabel = Math.max(4, Math.floor(slot / 7));
  const short = (name: string) => (name.length > maxLabel ? `${name.slice(0, maxLabel - 1)}…` : name);
  const date = (value: string) => formatDate(value, dateLocale(locale));
  const hovered = hover === null ? null : sprints[hover];
  const share = (committed: number | null, completed: number | null) => percent(completed ?? 0, committed ?? 0);

  return (
    <section className={`${styles.velocity} ${charts.palette}`} aria-labelledby="velocity-title">
      <figure className={charts.chart}>
        <div className={charts.titleRow}>
          <div className={styles.velocityHead}>
            <figcaption id="velocity-title" className={styles.sectionName}>
              {t("sprints.velocity.title")}
            </figcaption>
            <span className={styles.sectionMeta}>
              {t("sprints.velocity.average", { count: data.averageCompleted })}
            </span>
          </div>
          <ul className={charts.legend}>
            <li>
              <span className={charts.swatch} style={{ background: "var(--series-rest)" }} aria-hidden="true" />
              {t("sprints.velocity.committed")}
            </li>
            <li>
              <span className={charts.swatch} style={{ background: "var(--series-1)" }} aria-hidden="true" />
              {t("sprints.velocity.completed")}
            </li>
            <li>
              <span className={`${charts.lineKey} ${charts.lineKeyDashed}`} aria-hidden="true" />
              {t("sprints.velocity.averageKey")}
            </li>
          </ul>
        </div>

        <div ref={plotRef} className={charts.plot} onPointerLeave={() => setHover(null)}>
          {width > 0 ? (
            <svg
              className={charts.burndown}
              width={width}
              height={HEIGHT}
              role="img"
              aria-label={t("sprints.velocity.summary", { count: sprints.length, average: data.averageCompleted })}
            >
              {ticks.map((tick) => (
                <g key={tick}>
                  <line className={charts.grid} x1={PAD.left} x2={PAD.left + plotWidth} y1={y(tick)} y2={y(tick)} />
                  <text className={charts.axis} x={PAD.left - 8} y={y(tick)} textAnchor="end" dominantBaseline="middle">
                    {tick}
                  </text>
                </g>
              ))}

              {sprints.map((sprint, index) => {
                const committed = sprint.committedPoints ?? 0;
                const completed = sprint.completedPoints ?? 0;
                const left = center(index) - BAR - GAP / 2;

                return (
                  <g
                    key={sprint.id}
                    className={styles.velocityGroup}
                    onPointerEnter={() => setHover(index)}
                    onClick={() => onOpen(sprint.id)}
                  >
                    <rect className={styles.velocityHit} x={PAD.left + slot * index} y={PAD.top} width={slot} height={plotHeight} />
                    <path d={bar(left, y(committed), BAR, y(0) - y(committed))} style={{ fill: "var(--series-rest)" }} />
                    <path d={bar(left + BAR + GAP, y(completed), BAR, y(0) - y(completed))} style={{ fill: "var(--series-1)" }} />
                    <text className={charts.barValue} x={left + BAR / 2} y={y(committed) - 6} textAnchor="middle">
                      {committed}
                    </text>
                    <text className={charts.barValue} x={left + BAR + GAP + BAR / 2} y={y(completed) - 6} textAnchor="middle">
                      {completed}
                    </text>
                    <text className={charts.axis} x={center(index)} y={HEIGHT - 8} textAnchor="middle">
                      {short(sprint.name)}
                    </text>
                  </g>
                );
              })}

              <line
                className={charts.idealLine}
                x1={PAD.left}
                x2={PAD.left + plotWidth}
                y1={y(data.averageCompleted)}
                y2={y(data.averageCompleted)}
              />
            </svg>
          ) : (
            <div style={{ height: HEIGHT }} />
          )}

          {hovered && hover !== null ? (
            <div
              className={charts.tooltip}
              role="tooltip"
              style={{
                left: Math.min(Math.max(center(hover), 90), width - 90),
                top: y(Math.max(hovered.committedPoints ?? 0, hovered.completedPoints ?? 0)),
              }}
            >
              <strong>{hovered.name}</strong>
              <span>{t("sprints.dates", { start: date(hovered.startDate), end: date(hovered.endDate) })}</span>
              <span>
                {t("sprints.velocity.committed")}: {t("sprints.pointsShort", { count: hovered.committedPoints ?? 0 })}
              </span>
              <span>
                {t("sprints.velocity.completed")}: {t("sprints.pointsShort", { count: hovered.completedPoints ?? 0 })} (
                {share(hovered.committedPoints, hovered.completedPoints)}%)
              </span>
            </div>
          ) : null}
        </div>

        <details className={charts.dataTable}>
          <summary>{t("sprints.burndown.showTable")}</summary>
          <table className={charts.table}>
            <thead>
              <tr>
                <th scope="col">{t("sprints.velocity.sprint")}</th>
                <th scope="col" className={charts.num}>{t("sprints.velocity.committed")}</th>
                <th scope="col" className={charts.num}>{t("sprints.velocity.completed")}</th>
                <th scope="col" className={charts.num}>%</th>
              </tr>
            </thead>
            <tbody>
              {sprints.map((sprint) => (
                <tr key={sprint.id}>
                  <th scope="row">{sprint.name}</th>
                  <td className={charts.num}>{sprint.committedPoints ?? 0}</td>
                  <td className={charts.num}>{sprint.completedPoints ?? 0}</td>
                  <td className={charts.num}>{share(sprint.committedPoints, sprint.completedPoints)}%</td>
                </tr>
              ))}
            </tbody>
          </table>
        </details>
      </figure>
    </section>
  );
}
