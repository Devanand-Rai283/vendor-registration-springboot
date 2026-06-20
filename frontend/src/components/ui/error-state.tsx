import React from "react";
import { AlertCircle, RotateCcw } from "lucide-react";
import { Button } from "./button";
import { formatErrorMessage } from "@/utils/error";

interface ErrorStateProps {
  error: unknown;
  title?: string;
  onRetry?: () => void;
  className?: string;
}

export function ErrorState({
  error,
  title = "Failed to load data",
  onRetry,
  className,
}: ErrorStateProps) {
  const displayMessage = formatErrorMessage(error);

  return (
    <div
      className={`flex min-h-[350px] flex-col items-center justify-center border border-red-100 bg-red-50/20 p-8 text-center rounded-2xl ${
        className || ""
      }`}
    >
      <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger/10 text-danger mb-4">
        <AlertCircle className="h-6 w-6" />
      </div>

      <h3 className="text-lg font-bold text-text-primary">{title}</h3>
      <p className="mt-2 text-sm text-red-600/80 max-w-sm font-medium">
        {displayMessage}
      </p>

      {/* Optional retry button */}
      {onRetry && (
        <Button
          onClick={onRetry}
          variant="outline"
          size="sm"
          className="mt-6 border-red-200 text-red-700 hover:bg-red-50"
        >
          <RotateCcw className="mr-2 h-4 w-4" /> Try Again
        </Button>
      )}
    </div>
  );
}

export default ErrorState;
