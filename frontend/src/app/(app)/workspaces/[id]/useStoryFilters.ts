"use client";

import { useSearchParams } from "next/navigation";
import { useEffect, useState } from "react";
import { EMPTY_STORY_FILTERS, type StoryFilters } from "@/lib/story-filters";

export function useStoryFilters() {
  const params = useSearchParams();
  const [filters, setFilters] = useState<StoryFilters>({ ...EMPTY_STORY_FILTERS, text: params.get("q") ?? "" });

  useEffect(() => {
    setFilters((current) => ({ ...current, text: params.get("q") ?? "" }));
  }, [params]);

  return [filters, setFilters] as const;
}
