import { cn } from "@/lib/utils";

type StatusVariant = "SUCCESS" | "WARNING" | "DANGER" | "INFO";

interface StatusChipProps {
  status: StatusVariant;
  label: string;
  className?: string;
}

const statusStyles: Record<StatusVariant, string> = {
  SUCCESS:
    "bg-success/15 text-success border border-success/30",
  WARNING:
    "bg-warning/15 text-warning border border-warning/30",
  DANGER:
    "bg-danger/15 text-danger border border-danger/30",
  INFO:
    "bg-street-blue/15 text-street-blue border border-street-blue/30",
};

const dotStyles: Record<StatusVariant, string> = {
  SUCCESS: "bg-success",
  WARNING: "bg-warning",
  DANGER: "bg-danger",
  INFO: "bg-street-blue",
};

export function StatusChip({ status, label, className }: StatusChipProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center gap-1.5 rounded-full px-2.5 py-0.5 text-xs font-semibold",
        statusStyles[status],
        className
      )}
    >
      <span
        className={cn(
          "h-1.5 w-1.5 rounded-full",
          dotStyles[status]
        )}
        aria-hidden="true"
      />
      {label}
    </span>
  );
}

export type { StatusVariant };
export default StatusChip;
