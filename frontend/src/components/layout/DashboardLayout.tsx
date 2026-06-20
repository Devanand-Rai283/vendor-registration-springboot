"use client";

import React from "react";
import { cn } from "@/lib/utils";

interface DashboardLayoutProps {
  children: React.ReactNode; // Content Slot
  sidebar?: React.ReactNode;  // Sidebar Slot
  header?: React.ReactNode;   // Header Slot
}

export default function DashboardLayout({
  children,
  sidebar,
  header,
}: DashboardLayoutProps) {
  return (
    <div className="flex min-h-screen bg-background text-text-primary">
      {/* Sidebar Slot Container */}
      {sidebar && (
        <aside className="fixed inset-y-0 left-0 z-40 w-64 border-r border-border bg-surface">
          {sidebar}
        </aside>
      )}

      {/* Main Content Area Wrapper */}
      <div className={cn("flex flex-1 flex-col", sidebar ? "pl-64" : "")}>
        {/* Header Slot Container */}
        {header && (
          <header className="sticky top-0 z-30 h-16 border-b border-border bg-surface px-8 flex items-center justify-between">
            {header}
          </header>
        )}

        {/* Content Area Slot (Dashboard width capped at 1280px) */}
        <main className="flex-1 py-8">
          <div className="mx-auto max-w-[1280px] px-8 w-full">
            {children}
          </div>
        </main>
      </div>
    </div>
  );
}
