import React from "react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";
import { Clock } from "lucide-react";

interface OrderCardProps {
  className?: string;
}

export function OrderCard({ className }: OrderCardProps) {
  return (
    <Card className={cn("overflow-hidden", className)}>
      <CardContent className="p-6">
        <div className="flex flex-col items-center justify-center py-8 text-center">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-muted text-text-secondary mb-4">
            <Clock className="h-6 w-6" />
          </div>
          <h3 className="text-lg font-semibold text-text-primary">
            Order Tracking
          </h3>
          <p className="mt-2 text-sm text-text-secondary max-w-sm">
            Order management and tracking will be available once the ordering
            system is integrated.
          </p>
        </div>
      </CardContent>
    </Card>
  );
}

export default OrderCard;
