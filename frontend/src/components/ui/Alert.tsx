import { AlertCircle, Info } from "lucide-react";
import type { ReactNode } from "react";
import styles from "./Alert.module.css";

type AlertProps = {
  tone?: "danger" | "info";
  children: ReactNode;
};

export function Alert({ tone = "danger", children }: AlertProps) {
  const Icon = tone === "danger" ? AlertCircle : Info;

  return (
    <div className={`${styles.alert} ${styles[tone]}`} role={tone === "danger" ? "alert" : "status"}>
      <Icon className={styles.icon} size={16} aria-hidden="true" />
      <div>{children}</div>
    </div>
  );
}
