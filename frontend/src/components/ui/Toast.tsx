"use client";

import * as RadixToast from "@radix-ui/react-toast";
import { AlertCircle, CheckCircle2, Info, X } from "lucide-react";
import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from "react";
import { useT } from "@/i18n/client";
import styles from "./Toast.module.css";

type ToastTone = "success" | "danger" | "info";
type ToastItem = { id: number; tone: ToastTone; title: string; description?: string };
type ToastInput = Omit<ToastItem, "id" | "tone"> & { tone?: ToastTone };

const ToastContext = createContext<((toast: ToastInput) => void) | null>(null);

const ICONS = { success: CheckCircle2, danger: AlertCircle, info: Info } as const;

export function useToast() {
  const push = useContext(ToastContext);

  if (!push) {
    throw new Error("useToast must be used inside <ToastProvider>");
  }

  return push;
}

export function ToastProvider({ children }: { children: ReactNode }) {
  const t = useT();
  const [items, setItems] = useState<ToastItem[]>([]);

  const push = useCallback((input: ToastInput) => {
    setItems((current) => [...current, { id: Date.now() + Math.random(), tone: input.tone ?? "info", title: input.title, description: input.description }]);
  }, []);

  const value = useMemo(() => push, [push]);

  return (
    <ToastContext.Provider value={value}>
      <RadixToast.Provider swipeDirection="right" duration={5000}>
        {children}
        {items.map((item) => {
          const Icon = ICONS[item.tone];

          return (
            <RadixToast.Root
              key={item.id}
              className={`${styles.toast} ${styles[item.tone]}`}
              onOpenChange={(open) => {
                if (!open) setItems((current) => current.filter((candidate) => candidate.id !== item.id));
              }}
            >
              <Icon className={styles.icon} size={18} aria-hidden="true" />
              <div className={styles.text}>
                <RadixToast.Title className={styles.title}>{item.title}</RadixToast.Title>
                {item.description ? (
                  <RadixToast.Description className={styles.description}>{item.description}</RadixToast.Description>
                ) : null}
              </div>
              <RadixToast.Close className={styles.close} aria-label={t("common.close")}>
                <X size={14} aria-hidden="true" />
              </RadixToast.Close>
            </RadixToast.Root>
          );
        })}
        <RadixToast.Viewport className={styles.viewport} />
      </RadixToast.Provider>
    </ToastContext.Provider>
  );
}
