import { Skeleton } from "@/components/ui/Skeleton";
import styles from "./sprints.module.css";

export default function SprintsLoading() {
  return (
    <ul className={styles.grid} aria-busy="true">
      {Array.from({ length: 6 }, (_, index) => (
        <li key={index} className={styles.previewSkeleton}>
          <Skeleton width="40%" />
          <Skeleton width="60%" />
          <Skeleton width="100%" />
        </li>
      ))}
    </ul>
  );
}
