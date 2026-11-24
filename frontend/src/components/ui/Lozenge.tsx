import type { ReactNode } from "react";
import { Badge, type BadgeTone } from "./Badge";

type LozengeProps = {
  tone?: "neutral" | "blue" | "green" | "yellow";
  children: ReactNode;
};

const TONES: Record<NonNullable<LozengeProps["tone"]>, BadgeTone> = {
  neutral: "neutral",
  blue: "primary",
  green: "success",
  yellow: "warning",
};

/** Kept for existing call sites; new code should use Badge. */
export function Lozenge({ tone = "neutral", children }: LozengeProps) {
  return <Badge tone={TONES[tone]}>{children}</Badge>;
}
