import React from "react";
import { cn } from "@/lib/utils";

interface SidebarShellProps {
  sidebar: React.ReactNode;
  children: React.ReactNode;
  className?: string;
  sidebarWidth?: string;
}

export function SidebarShell({
  sidebar,
  children,
  className,
  sidebarWidth = "w-64",
}: SidebarShellProps) {
  return (
    <div className={cn("flex gap-0", className)}>
      <aside
        className={cn(
          "hidden lg:block shrink-0 border-r border-border bg-surface",
          sidebarWidth
        )}
      >
        <div className="sticky top-20 h-[calc(100vh-5rem)] overflow-y-auto p-4">
          {sidebar}
        </div>
      </aside>
      <main className="flex-1 min-w-0 p-4 lg:p-8">{children}</main>
    </div>
  );
}

export default SidebarShell;
