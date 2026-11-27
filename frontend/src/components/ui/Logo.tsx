import styles from "./Logo.module.css";

type LogoProps = {
  size?: "md" | "lg";
  /** "stacked" puts the name under the heron as a wordmark, for a page's brand block such as the login card. */
  layout?: "inline" | "stacked";
};

/** The heron mark and the name. Both drawings are loaded and the theme shows one, so there is no flash on first paint. */
export function Logo({ size = "md", layout = "inline" }: LogoProps) {
  return (
    <span className={`${styles.logo} ${styles[size]} ${styles[layout]}`}>
      <img className={`${styles.mark} ${styles.light}`} src="/brand/prumo-garca.svg" alt="" aria-hidden="true" />
      <img className={`${styles.mark} ${styles.dark}`} src="/brand/prumo-garca-dark.svg" alt="" aria-hidden="true" />
      <span className={styles.name}>Prumo</span>
    </span>
  );
}
