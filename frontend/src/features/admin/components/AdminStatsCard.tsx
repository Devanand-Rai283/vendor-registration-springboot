import React from "react";
import { Card, CardContent } from "@/components/ui/card";
import { cn } from "@/lib/utils";

interface AdminStatsCardProps {
  title: string;
  value: string | number;
  icon: React.ReactNode;
  description?: string;
  className?: string;
}

export function AdminStatsCard({
  title,
  value,
  icon,
  description,
  className,
}: AdminStatsCardProps) {
  return (
    <Card className={cn("overflow-hidden transition-all duration-300 hover:shadow-md border-border bg-surface", className)}>
      <CardContent className="p-6">
        <div className="flex items-center justify-between">
          <div className="space-y-1">
            <p className="text-sm font-semibold text-text-secondary tracking-wide uppercase">
              {title}
            </p>
            <p className="text-3xl font-extrabold tracking-tight text-text-primary">
              {value}
            </p>
          </div>
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-street-blue/10 text-street-blue">
            {icon}
          </div>
        </div>
        {description && (
          <p className="mt-4 text-xs font-medium text-text-secondary">
            {description}
          </p>
        )}
      </CardContent>
    </Card>
  );
}
