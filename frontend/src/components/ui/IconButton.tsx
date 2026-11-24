import type { ButtonHTMLAttributes, ReactNode } from "react";
import { Tooltip } from "./Tooltip";
import styles from "./IconButton.module.css";

type IconButtonProps = Omit<ButtonHTMLAttributes<HTMLButtonElement>, "aria-label" | "children"> & {
  /** Required: icon-only buttons have no visible text, so this is the accessible name and the tooltip. */
  label: string;
  icon: ReactNode;
  size?: "md" | "sm";
};

export function IconButton({ label, icon, size = "md", className, type = "button", ...rest }: IconButtonProps) {
  return (
    <Tooltip content={label}>
      <button
        type={type}
        aria-label={label}
        className={[styles.button, size === "sm" ? styles.sm : "", className ?? ""].join(" ")}
        {...rest}
      >
        {icon}
      </button>
    </Tooltip>
  );
}
