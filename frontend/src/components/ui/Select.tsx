import { ChevronDown } from "lucide-react";
import type { SelectHTMLAttributes } from "react";
import styles from "./Select.module.css";

type SelectProps = SelectHTMLAttributes<HTMLSelectElement> & {
  compact?: boolean;
  /** Visual style: "field" has a border, "quiet" is a filled chip for toolbars and inline editing. */
  variant?: "field" | "quiet";
};

export function Select({ compact = false, variant = "field", className, children, ...rest }: SelectProps) {
  const classes = [styles.wrap, compact ? styles.compact : "", variant === "quiet" ? styles.quiet : "", className ?? ""].join(" ");

  return (
    <span className={classes}>
      <select className={styles.select} {...rest}>
        {children}
      </select>
      <ChevronDown className={styles.chevron} size={14} aria-hidden="true" />
    </span>
  );
}
