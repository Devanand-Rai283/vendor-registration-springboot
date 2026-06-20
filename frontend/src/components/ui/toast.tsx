"use client";

import React, { createContext, useContext, useState, useCallback } from "react";
import { cn } from "@/lib/utils";
import { X, CheckCircle2, AlertCircle, AlertTriangle, Info } from "lucide-react";

type ToastType = "success" | "error" | "warning" | "info";

interface Toast {
  id: string;
  type: ToastType;
  title: string;
  description?: string;
}

interface ToastContextValue {
  toasts: Toast[];
  addToast: (toast: Omit<Toast, "id">) => void;
  removeToast: (id: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

const toastStyles: Record<ToastType, { container: string; icon: React.ReactNode }> = {
  success: {
    container: "border-success/20 bg-success/5",
    icon: <CheckCircle2 className="h-5 w-5 text-success" />,
  },
  error: {
    container: "border-danger/20 bg-danger/5",
    icon: <AlertCircle className="h-5 w-5 text-danger" />,
  },
  warning: {
    container: "border-warning/20 bg-warning/5",
    icon: <AlertTriangle className="h-5 w-5 text-warning" />,
  },
  info: {
    container: "border-street-blue/20 bg-street-blue/5",
    icon: <Info className="h-5 w-5 text-street-blue" />,
  },
};

export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<Toast[]>([]);

  const addToast = useCallback((toast: Omit<Toast, "id">) => {
    const id = `toast-${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    setToasts((prev) => [...prev, { ...toast, id }]);

    setTimeout(() => {
      setToasts((prev) => prev.filter((t) => t.id !== id));
    }, 5000);
  }, []);

  const removeToast = useCallback((id: string) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  return (
    <ToastContext.Provider value={{ toasts, addToast, removeToast }}>
      {children}
      {toasts.length > 0 && (
        <div
          aria-live="polite"
          aria-label="Notifications"
          className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 max-w-sm w-full pointer-events-none"
        >
          {toasts.map((toast) => {
            const style = toastStyles[toast.type];
            return (
              <div
                key={toast.id}
                role="alert"
                className={cn(
                  "pointer-events-auto flex items-start gap-3 rounded-xl border p-4 shadow-lg backdrop-blur-sm animate-in slide-in-from-right-2 fade-in duration-300",
                  style.container,
                  "bg-surface/95"
                )}
              >
                <div className="mt-0.5 shrink-0">{style.icon}</div>
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-semibold text-text-primary">
                    {toast.title}
                  </p>
                  {toast.description && (
                    <p className="mt-0.5 text-sm text-text-secondary">
                      {toast.description}
                    </p>
                  )}
                </div>
                <button
                  onClick={() => removeToast(toast.id)}
                  className="shrink-0 rounded-lg p-1 text-text-secondary hover:bg-muted hover:text-text-primary transition-colors"
                  aria-label="Dismiss notification"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>
            );
          })}
        </div>
      )}
    </ToastContext.Provider>
  );
}

export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast must be used within a ToastProvider");
  }
  return ctx;
}

export default ToastProvider;
