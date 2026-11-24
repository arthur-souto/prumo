import { ChevronDown, ChevronUp, ChevronsUp, Equal } from "lucide-react";
import type { Priority } from "@/lib/board-types";
import styles from "./board.module.css";

const ICONS = { URGENT: ChevronsUp, HIGH: ChevronUp, MEDIUM: Equal, LOW: ChevronDown } as const;

export function PriorityBadge({ priority, label }: { priority: Priority; label: string }) {
  const Icon = ICONS[priority];

  return (
    <span className={`${styles.priority} ${styles[`priority${priority}`]}`} title={label}>
      <Icon size={16} strokeWidth={2.25} aria-hidden="true" />
      <span className={styles.srOnly}>{label}</span>
    </span>
  );
}
