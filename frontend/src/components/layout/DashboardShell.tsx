import React from "react";
import { cn } from "@/lib/utils";

interface DashboardShellProps {
  children: React.ReactNode;
  className?: string;
}

export function DashboardShell({ children, className }: DashboardShellProps) {
  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Customer navigation entry via TopNav if used in DashboardShell context */}
      <div
        className={cn(
          "mx-auto w-full max-w-[1280px] px-4 sm:px-6 lg:px-8 py-8 flex-1",
          className
        )}
      >
        {children}
      </div>
    </div>
  );
}

export default DashboardShell;
