import { Skeleton } from "@/components/ui/Skeleton";
import styles from "./board.module.css";

export default function BoardLoading() {
  return (
    <div className={styles.skeletonBoard} aria-busy="true">
      {Array.from({ length: 4 }, (_, column) => (
        <div key={column} className={styles.skeletonColumn}>
          <Skeleton width="6rem" height="0.875rem" />
          {Array.from({ length: 3 - (column % 2) }, (_, card) => (
            <Skeleton key={card} height="4.5rem" />
          ))}
        </div>
      ))}
    </div>
  );
}
