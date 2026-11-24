"use client";

import { ErrorState } from "@/components/ui/ErrorState";

export default function AppError({ reset }: { error: Error; reset: () => void }) {
  return <ErrorState onRetry={reset} />;
}
