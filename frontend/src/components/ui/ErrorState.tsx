"use client";

import { RotateCw, TriangleAlert } from "lucide-react";
import { useT } from "@/i18n/client";
import { Button } from "./Button";
import { EmptyState } from "./EmptyState";

type ErrorStateProps = {
  title?: string;
  description?: string;
  onRetry?: () => void;
  compact?: boolean;
};

export function ErrorState({ title, description, onRetry, compact }: ErrorStateProps) {
  const t = useT();

  return (
    <EmptyState
      compact={compact}
      icon={<TriangleAlert size={20} aria-hidden="true" />}
      title={title ?? t("common.errorTitle")}
      description={description ?? t("common.errorText")}
      action={
        onRetry ? (
          <Button icon={<RotateCw size={14} aria-hidden="true" />} onClick={onRetry}>
            {t("common.retry")}
          </Button>
        ) : undefined
      }
    />
  );
}
