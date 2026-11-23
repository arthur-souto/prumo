import styles from "./WorkspaceIcon.module.css";

type WorkspaceIconProps = {
  id: string;
  name: string;
  size?: "sm" | "lg";
};

function paletteIndex(id: string) {
  let hash = 0;
  for (const char of id) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return (hash % 6) + 1;
}

export function WorkspaceIcon({ id, name, size = "sm" }: WorkspaceIconProps) {
  return (
    <span className={`${styles.icon} ${styles[size]}`} style={{ background: `var(--avatar-${paletteIndex(id)})` }} aria-hidden="true">
      {name.trim().charAt(0).toUpperCase()}
    </span>
  );
}
