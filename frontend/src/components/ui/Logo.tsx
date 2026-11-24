import styles from "./Logo.module.css";

type LogoProps = {
  size?: "md" | "lg";
};

export function Logo({ size = "md" }: LogoProps) {
  return (
    <span className={`${styles.logo} ${styles[size]}`}>
      <svg className={styles.mark} viewBox="0 0 40 40" aria-hidden="true">
        <rect width="40" height="40" rx="6" fill="var(--logo-bg)" />
        <circle cx="20" cy="9" r="2.4" fill="none" stroke="var(--logo-accent)" strokeWidth="1.8" />
        <path d="M20 11.4v8.1" stroke="var(--logo-accent)" strokeWidth="1.8" strokeLinecap="round" />
        <path d="M20 17.5 27.5 26 20 34.5 12.5 26z" fill="var(--logo-accent)" opacity="0.38" />
        <path d="M20 19.5 25.2 26 20 32.5 14.8 26z" fill="var(--logo-accent)" />
      </svg>
      <span className={styles.name}>Prumo</span>
    </span>
  );
}
