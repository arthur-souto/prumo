import { GithubIcon } from "@/components/ui/GithubIcon";
import buttonStyles from "@/components/ui/Button.module.css";

export function GithubButton({ label }: { label: string }) {
  return (
    <a
      href="/api/auth/github"
      className={`${buttonStyles.button} ${buttonStyles.default} ${buttonStyles.full}`}
      style={{ textDecoration: "none" }}
    >
      <GithubIcon />
      <span>{label}</span>
    </a>
  );
}
