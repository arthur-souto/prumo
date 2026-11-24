"use client";

import * as RadixTabs from "@radix-ui/react-tabs";
import type { ComponentProps } from "react";
import styles from "./Tabs.module.css";

export const Tabs = RadixTabs.Root;

export function TabsList(props: ComponentProps<typeof RadixTabs.List>) {
  return <RadixTabs.List {...props} className={`${styles.list} ${props.className ?? ""}`} />;
}

export function TabsTrigger(props: ComponentProps<typeof RadixTabs.Trigger>) {
  return <RadixTabs.Trigger {...props} className={`${styles.trigger} ${props.className ?? ""}`} />;
}

export function TabsContent(props: ComponentProps<typeof RadixTabs.Content>) {
  return <RadixTabs.Content {...props} className={`${styles.content} ${props.className ?? ""}`} />;
}
