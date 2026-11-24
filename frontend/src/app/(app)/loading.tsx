import { Skeleton } from "@/components/ui/Skeleton";
import styles from "./workspaces.module.css";

export default function AppLoading() {
  return (
    <div className={styles.page} aria-busy="true">
      <Skeleton width="12rem" height="1.75rem" />
      <div className={styles.skeletonGrid}>
        {Array.from({ length: 6 }, (_, index) => (
          <Skeleton key={index} height="7rem" />
        ))}
      </div>
    </div>
  );
}
