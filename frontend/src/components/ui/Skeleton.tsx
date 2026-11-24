import type { CSSProperties } from "react";
import styles from "./Skeleton.module.css";

type SkeletonProps = {
  width?: CSSProperties["width"];
  height?: CSSProperties["height"];
  className?: string;
};

export function Skeleton({ width = "100%", height = "1rem", className }: SkeletonProps) {
  return <span className={`${styles.skeleton} ${className ?? ""}`} style={{ width, height }} aria-hidden="true" />;
}

export function SkeletonText({ lines = 3 }: { lines?: number }) {
  return (
    <span className={styles.text} aria-hidden="true">
      {Array.from({ length: lines }, (_, index) => (
        <Skeleton key={index} width={index === lines - 1 ? "60%" : "100%"} />
      ))}
    </span>
  );
}
