"use client";

import { useSearchParams } from "next/navigation";
import { useCallback } from "react";
import { replaceSearchParams } from "@/lib/url-state";

export function useStoryParam() {
  const params = useSearchParams();
  const key = params.get("story");

  const clear = useCallback(() => replaceSearchParams((current) => current.delete("story")), []);

  return [key, clear] as const;
}
