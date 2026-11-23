import type { ReactNode } from "react";
import styles from "./Alert.module.css";

type AlertProps = {
  tone?: "danger" | "info";
  children: ReactNode;
};

export function Alert({ tone = "danger", children }: AlertProps) {
  return (
    <div className={`${styles.alert} ${styles[tone]}`} role={tone === "danger" ? "alert" : "status"}>
      {children}
    </div>
  );
}
