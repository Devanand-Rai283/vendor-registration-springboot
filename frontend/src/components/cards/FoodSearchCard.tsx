import React from "react";
import Link from "next/link";
import { Star, Store } from "lucide-react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";

interface FoodSearchCardProps {
  menuItemId: string;
  itemName: string;
  description: string;
  price: number;
  dietaryTag?: string;
  vendorId: string;
  vendorName: string;
  foodType: string;
  averageRating: number;
  className?: string;
}

export function FoodSearchCard({
  itemName,
  description,
  price,
  dietaryTag,
  vendorId,
  vendorName,
  foodType,
  averageRating,
  className,
}: FoodSearchCardProps) {
  return (
    <Card className={cn("overflow-hidden transition-all duration-200 hover:shadow-md", className)}>
      <CardContent className="p-4 flex flex-col h-full justify-between">
        <div>
          <div className="flex items-start justify-between gap-2 mb-2">
            <div className="min-w-0">
              <h4 className="font-semibold text-text-primary text-base leading-tight truncate pr-2">
                {itemName}
              </h4>
              {dietaryTag && (
                <div className="mt-1.5">
                  <span className="inline-flex items-center rounded-md bg-vendor-orange/10 px-2 py-0.5 text-xs font-medium text-vendor-orange ring-1 ring-inset ring-vendor-orange/20">
                    {dietaryTag}
                  </span>
                </div>
              )}
            </div>
            <span className="shrink-0 text-sm font-bold text-text-primary whitespace-nowrap bg-muted px-2 py-1 rounded-md">
              KES {price.toLocaleString()}
            </span>
          </div>
          <p className="text-sm text-text-secondary line-clamp-2 mt-2">
            {description}
          </p>
        </div>

        <div className="mt-4 pt-3 border-t border-border/60">
          <Link 
            href={`/vendors/${vendorId}`} 
            className="group flex items-center justify-between rounded-md p-1.5 -mx-1.5 hover:bg-muted transition-colors"
          >
            <div className="flex items-center gap-2 overflow-hidden">
              <div className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-street-blue/10 text-street-blue">
                <Store className="h-4 w-4" />
              </div>
              <div className="min-w-0">
                <p className="text-sm font-medium text-text-primary truncate group-hover:text-street-blue transition-colors">
                  {vendorName}
                </p>
                <p className="text-xs text-text-secondary truncate">
                  {foodType}
                </p>
              </div>
            </div>
            <div className="flex items-center gap-1 text-amber-600 bg-amber-50 px-1.5 py-0.5 rounded text-xs font-medium shrink-0">
              <Star className="h-3 w-3 fill-current" />
              {averageRating.toFixed(1)}
            </div>
          </Link>
        </div>
      </CardContent>
    </Card>
  );
}

export default FoodSearchCard;
