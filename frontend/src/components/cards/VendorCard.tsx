import React from "react";
import Image from "next/image";
import Link from "next/link";
import { Star, MapPin } from "lucide-react";
import { cn } from "@/lib/utils";
import { Card, CardContent } from "@/components/ui/card";

interface VendorCardProps {
  id: string;
  name: string;
  rating: number;
  distance?: number;
  foodType: string;
  imageUrl?: string;
  className?: string;
}

export function VendorCard({
  id,
  name,
  rating,
  distance,
  foodType,
  imageUrl,
  className,
}: VendorCardProps) {
  return (
    <Link href={`/vendors/${id}`} className={cn("group block", className)}>
      <Card className="overflow-hidden transition-all duration-200 hover:shadow-lg hover:-translate-y-0.5">
        <div className="relative h-40 w-full bg-muted overflow-hidden">
          {imageUrl ? (
            <Image
              src={imageUrl}
              alt={name}
              fill
              className="object-cover transition-transform duration-300 group-hover:scale-105"
              sizes="(max-width: 768px) 100vw, (max-width: 1200px) 50vw, 33vw"
            />
          ) : (
            <div className="flex h-full items-center justify-center bg-gradient-to-br from-street-blue/10 to-vendor-orange/10">
              <MapPin className="h-10 w-10 text-text-secondary/40" />
            </div>
          )}
        </div>
        <CardContent className="p-4">
          <h3 className="font-semibold text-text-primary group-hover:text-street-blue transition-colors truncate">
            {name}
          </h3>
          <p className="mt-0.5 text-sm text-text-secondary truncate">
            {foodType}
          </p>
          <div className="mt-2 flex items-center gap-3 text-sm">
            <span className="inline-flex items-center gap-1 text-amber-600 font-medium">
              <Star className="h-3.5 w-3.5 fill-current" />
              {rating.toFixed(1)}
            </span>
            {distance !== undefined && (
              <span className="inline-flex items-center gap-1 text-text-secondary">
                <MapPin className="h-3.5 w-3.5" />
                {distance < 1
                  ? `${(distance * 1000).toFixed(0)}m`
                  : `${distance.toFixed(1)}km`}
              </span>
            )}
          </div>
        </CardContent>
      </Card>
    </Link>
  );
}

export default VendorCard;
