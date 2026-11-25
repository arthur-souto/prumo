"use client";

import { User } from "lucide-react";
import { useRef, useState, type PointerEvent, type ReactNode } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { useT } from "@/i18n/client";
import type { AssigneeRow, StatusSlice } from "./sprint-metrics";
import { percent } from "./sprint-metrics";
import styles from "./sprint-charts.module.css";

type Tip = { x: number; y: number; content: ReactNode } | null;

function useTooltip() {
  const ref = useRef<HTMLDivElement>(null);
  const [tip, setTip] = useState<Tip>(null);

  function show(event: PointerEvent<HTMLElement>, content: ReactNode) {
    const box = ref.current?.getBoundingClientRect();
    if (!box) return;
    setTip({ x: event.clientX - box.left, y: event.clientY - box.top, content });
  }

  const node = tip ? (
    <div className={styles.tooltip} style={{ left: tip.x, top: tip.y }} role="tooltip">
      {tip.content}
    </div>
  ) : null;

  return { ref, show, hide: () => setTip(null), node };
}

export function StatusChart({ slices, total }: { slices: StatusSlice[]; total: number }) {
  const t = useT();
  const tooltip = useTooltip();
  const name = (slice: StatusSlice) => (slice.id === "other" ? t("sprints.other") : slice.name);
  const tip = (slice: StatusSlice) => (
    <>
      <strong>{name(slice)}</strong>
      <span>{t("sprints.tooltipPoints", { points: slice.points, share: percent(slice.points, total), count: slice.count })}</span>
    </>
  );

  return (
    <figure className={styles.chart}>
      <figcaption className={styles.title}>{t("sprints.byStatus")}</figcaption>
      <div ref={tooltip.ref} className={styles.plot} onPointerLeave={tooltip.hide}>
        <div className={styles.segments} role="img" aria-label={t("sprints.byStatus")}>
          {slices
            .filter((slice) => slice.points > 0)
            .map((slice) => (
              <span
                key={slice.id}
                className={styles.segment}
                style={{ flexGrow: slice.points, background: `var(--series-${slice.slot})` }}
                onPointerMove={(event) => tooltip.show(event, tip(slice))}
              />
            ))}
        </div>
        {tooltip.node}
      </div>
      <table className={styles.table}>
        <tbody>
          {slices.map((slice) => (
            <tr key={slice.id} data-empty={slice.points === 0 ? "true" : undefined}>
              <td className={styles.swatchCell}>
                <span className={styles.swatch} style={{ background: `var(--series-${slice.slot})` }} aria-hidden="true" />
              </td>
              <th scope="row">{name(slice)}</th>
              <td className={styles.num}>{t("sprints.pointsShort", { count: slice.points })}</td>
              <td className={`${styles.num} ${styles.share}`}>{percent(slice.points, total)}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    </figure>
  );
}

export function AssigneeChart({ rows }: { rows: AssigneeRow[] }) {
  const t = useT();
  const tooltip = useTooltip();
  const max = Math.max(1, ...rows.map((row) => row.done + row.remaining));

  return (
    <figure className={styles.chart}>
      <div className={styles.titleRow}>
        <figcaption className={styles.title}>{t("sprints.byAssignee")}</figcaption>
        <ul className={styles.legend}>
          <li>
            <span className={styles.swatch} style={{ background: "var(--series-1)" }} aria-hidden="true" />
            {t("sprints.doneSeries")}
          </li>
          <li>
            <span className={styles.swatch} style={{ background: "var(--series-rest)" }} aria-hidden="true" />
            {t("sprints.remainingSeries")}
          </li>
        </ul>
      </div>
      <div ref={tooltip.ref} className={styles.plot} onPointerLeave={tooltip.hide}>
        <ul className={styles.rows}>
          {rows.map((row) => {
            const name = row.member?.name ?? t("story.unassigned");
            const total = row.done + row.remaining;

            return (
              <li
                key={row.id ?? "none"}
                className={styles.row}
                onPointerMove={(event) =>
                  tooltip.show(
                    event,
                    <>
                      <strong>{name}</strong>
                      <span>{t("sprints.tooltipAssignee", { done: row.done, remaining: row.remaining, count: row.count })}</span>
                    </>,
                  )
                }
              >
                <span className={styles.who}>
                  {row.member ? (
                    <Avatar name={row.member.name} seed={row.member.id} src={row.member.avatarUrl} size="sm" />
                  ) : (
                    <span className={styles.nobody}>
                      <User size={12} aria-hidden="true" />
                    </span>
                  )}
                  <span className={styles.whoName}>{name}</span>
                  <span className={styles.value}>
                    {row.done}/{total}
                  </span>
                </span>
                <span className={styles.stack} style={{ width: `${(total / max) * 100}%` }}>
                  {row.done > 0 ? <span className={styles.bar} style={{ flexGrow: row.done, background: "var(--series-1)" }} /> : null}
                  {row.remaining > 0 ? <span className={styles.bar} style={{ flexGrow: row.remaining, background: "var(--series-rest)" }} /> : null}
                </span>
              </li>
            );
          })}
        </ul>
        {tooltip.node}
      </div>
    </figure>
  );
}
