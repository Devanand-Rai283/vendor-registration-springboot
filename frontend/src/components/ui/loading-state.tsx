import React from "react";
import { Spinner } from "./spinner";

interface LoadingStateProps {
  message?: string;
  className?: string;
}

export function LoadingState({
  message = "Fetching resources, please wait...",
  className,
}: LoadingStateProps) {
  return (
    <div
      className={`flex min-h-[350px] flex-col items-center justify-center gap-3 p-8 text-center rounded-2xl border border-dashed border-border bg-surface/50 backdrop-blur-xs ${
        className || ""
      }`}
    >
      <Spinner size="lg" />
      <p className="text-sm font-semibold text-text-secondary tracking-wide animate-pulse">
        {message}
      </p>
    </div>
  );
}

export default LoadingState;
