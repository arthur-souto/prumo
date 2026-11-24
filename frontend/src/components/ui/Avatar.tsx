import styles from "./Avatar.module.css";

type AvatarProps = {
  name: string;
  /** Stable key used to pick the colour, so the same person keeps the same colour everywhere. */
  seed?: string;
  src?: string | null;
  size?: "sm" | "md" | "lg";
};

function paletteIndex(seed: string) {
  let hash = 0;
  for (const char of seed) {
    hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  }
  return (hash % 6) + 1;
}

function initials(name: string) {
  const parts = name.trim().split(/\s+/).filter(Boolean);
  const letters = parts.length > 1 ? parts[0][0] + parts[parts.length - 1][0] : (parts[0] ?? "?").slice(0, 2);
  return letters.toUpperCase();
}

export function Avatar({ name, seed, src, size = "md" }: AvatarProps) {
  if (src) {
    // eslint-disable-next-line @next/next/no-img-element
    return <img className={`${styles.avatar} ${styles[size]}`} src={src} alt={name} />;
  }

  return (
    <span
      className={`${styles.avatar} ${styles[size]}`}
      style={{ background: `var(--avatar-${paletteIndex(seed ?? name)})` }}
      role="img"
      aria-label={name}
    >
      {initials(name)}
    </span>
  );
}
