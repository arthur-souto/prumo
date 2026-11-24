import type { ReactNode } from "react";
import styles from "./EmptyState.module.css";

type EmptyStateProps = {
  icon?: ReactNode;
  title: string;
  description?: string;
  /** A suggested next step, usually a Button. */
  action?: ReactNode;
  compact?: boolean;
};

export function EmptyState({ icon, title, description, action, compact = false }: EmptyStateProps) {
  return (
    <div className={`${styles.empty} ${compact ? styles.compact : ""}`}>
      {icon ? <span className={styles.icon}>{icon}</span> : null}
      <p className={styles.title}>{title}</p>
      {description ? <p className={styles.description}>{description}</p> : null}
      {action ? <div className={styles.action}>{action}</div> : null}
    </div>
  );
}
