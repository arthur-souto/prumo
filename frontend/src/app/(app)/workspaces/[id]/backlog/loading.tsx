import { Skeleton } from "@/components/ui/Skeleton";
import styles from "./backlog.module.css";

export default function BacklogLoading() {
  return (
    <div className={styles.wrap} aria-busy="true">
      {Array.from({ length: 8 }, (_, index) => (
        <div key={index} className={styles.skeletonRow}>
          <Skeleton width="4rem" />
          <Skeleton width="60%" />
          <Skeleton width="6rem" />
        </div>
      ))}
    </div>
  );
}
