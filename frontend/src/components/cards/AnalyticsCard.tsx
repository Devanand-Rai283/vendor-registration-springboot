import React from "react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { BarChart3 } from "lucide-react";

interface AnalyticsCardProps {
  className?: string;
}

export function AnalyticsCard({ className }: AnalyticsCardProps) {
  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardContent className="p-6">
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-muted text-text-secondary mb-4">
            <BarChart3 className="h-6 w-6" />
          </div>
          <h3 className="text-lg font-semibold text-text-primary">
            Analytics Dashboard
          </h3>
          <p className="mt-2 text-sm text-text-secondary max-w-sm">
            Performance metrics and insights will be displayed here once the
            analytics module is active.
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

export default AnalyticsCard;
