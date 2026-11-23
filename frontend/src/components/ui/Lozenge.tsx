import type { ReactNode } from "react";
import styles from "./Lozenge.module.css";

type LozengeProps = {
  tone?: "neutral" | "blue" | "green" | "yellow";
  children: ReactNode;
};

export function Lozenge({ tone = "neutral", children }: LozengeProps) {
  return <span className={`${styles.lozenge} ${styles[tone]}`}>{children}</span>;
}
