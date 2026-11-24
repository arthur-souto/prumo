import type { ButtonHTMLAttributes, ReactNode } from "react";
import styles from "./Button.module.css";

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  appearance?: "primary" | "secondary" | "default" | "subtle" | "danger";
  size?: "md" | "sm";
  fullWidth?: boolean;
  loading?: boolean;
  icon?: ReactNode;
};

export function Button({
  appearance = "secondary",
  size = "md",
  fullWidth = false,
  loading = false,
  icon,
  children,
  className,
  disabled,
  ...rest
}: ButtonProps) {
  const classes = [styles.button, styles[appearance === "default" ? "secondary" : appearance], size === "sm" ? styles.sm : "", fullWidth ? styles.full : "", className ?? ""].join(" ");

  return (
    <button className={classes} disabled={disabled || loading} aria-busy={loading || undefined} {...rest}>
      {loading ? <span className={styles.spinner} aria-hidden="true" /> : icon}
      <span>{children}</span>
    </button>
  );
}
