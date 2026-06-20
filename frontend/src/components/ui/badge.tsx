import * as React from "react";
import { cva, type VariantProps } from "class-variance-authority";
import { cn } from "@/lib/utils";

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-hidden focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-street-blue text-white shadow-xs hover:bg-blue-600",
        secondary:
          "border-transparent bg-vendor-orange text-white hover:bg-orange-600",
        success:
          "border-transparent bg-success/15 text-success border border-success/30",
        danger:
          "border-transparent bg-danger/15 text-danger border border-danger/30",
        warning:
          "border-transparent bg-warning/15 text-warning border border-warning/30",
        outline: "border-border text-text-primary bg-surface",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
);

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  );
}

export { Badge, badgeVariants };
