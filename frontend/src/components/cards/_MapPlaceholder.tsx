import React from "react";
import { MapPin } from "lucide-react";

export default function MapPlaceholder() {
  return (
    <div className="flex h-48 items-center justify-center bg-gradient-to-br from-street-blue/5 to-vendor-orange/5">
      <div className="flex flex-col items-center gap-2 text-text-secondary">
        <MapPin className="h-8 w-8" />
        <span className="text-sm font-medium">Map view will be rendered here</span>
        <span className="text-xs">Interactive map integration coming soon</span>
      </div>
    </div>
  );
}
