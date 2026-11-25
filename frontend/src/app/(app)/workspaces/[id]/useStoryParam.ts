"use client";

import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useCallback } from "react";

export function useStoryParam() {
  const params = useSearchParams();
  const pathname = usePathname();
  const router = useRouter();
  const key = params.get("story");

  const clear = useCallback(() => {
    if (!params.has("story")) return;

    const next = new URLSearchParams(params);
    next.delete("story");
    const query = next.toString();
    router.replace(query ? `${pathname}?${query}` : pathname, { scroll: false });
  }, [params, pathname, router]);

  return [key, clear] as const;
}
