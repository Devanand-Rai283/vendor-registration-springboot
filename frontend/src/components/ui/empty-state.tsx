import React from "react";
import { FolderOpen } from "lucide-react";
import { Button } from "./button";

interface EmptyStateProps {
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  icon?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  title = "No data found",
  description = "There are no records matching your request at this time.",
  actionLabel,
  onAction,
  icon,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={`flex min-h-[350px] flex-col items-center justify-center border border-dashed border-border bg-surface p-8 text-center rounded-2xl shadow-xs ${
        className || ""
      }`}
    >
      {/* Icon Wrapper */}
      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-muted text-text-secondary mb-4 shadow-inner">
        {icon || <FolderOpen className="h-6 w-6" />}
      </div>

      <h3 className="text-lg font-bold text-text-primary">{title}</h3>
      <p className="mt-2 text-sm text-text-secondary max-w-sm font-medium">
        {description}
      </p>

      {/* Optional action CTA */}
      {actionLabel && onAction && (
        <Button
          onClick={onAction}
          variant="default"
          size="sm"
          className="mt-6"
        >
          {actionLabel}
        </Button>
      )}
    </div>
  );
}

export default EmptyState;
