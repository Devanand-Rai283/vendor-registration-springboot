"use client";

import React, { useEffect, useRef, useCallback } from "react";
import type { Map as LeafletMap, LayerGroup } from "leaflet";
import "leaflet/dist/leaflet.css";

export interface MapVendor {
  id: string;
  name?: string;
  businessName?: string;
  latitude: number;
  longitude: number;
  foodType?: string;
  rating?: number;
}

interface VendorMapProps {
  vendors: MapVendor[];
  center?: [number, number];
  zoom?: number;
  fitBounds?: boolean;
  className?: string;
}

export default function VendorMap({
  vendors,
  center,
  zoom = 13,
  fitBounds = true,
  className = "h-[350px] w-full",
}: VendorMapProps) {
  const mapRef = useRef<HTMLDivElement>(null);
  const leafletMapRef = useRef<LeafletMap | null>(null);
  const markersLayerRef = useRef<LayerGroup | null>(null);

  // SVG store marker
  const createStoreIcon = useCallback((L: typeof import("leaflet"), name: string) => {
    return L.divIcon({
      className: "custom-leaflet-icon",
      html: `
        <div class="relative group flex flex-col items-center select-none" style="transform: translate(0, 0);">
          <div class="flex items-center justify-center w-8 h-8 rounded-full bg-blue-600 text-white shadow-md border-2 border-white transition-all duration-200 hover:scale-110 hover:bg-orange-500">
            <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="m2 7 4.4-4.4A2 2 0 0 1 7.8 2h8.4a2 2 0 0 1 1.4.6L22 7"/>
              <path d="M2 7v13a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V7"/>
              <path d="M12 22V12"/>
            </svg>
          </div>
          <div class="absolute -bottom-6 bg-slate-900/90 text-white text-[10px] px-1.5 py-0.5 rounded shadow whitespace-nowrap pointer-events-none transition-opacity duration-150">
            ${name}
          </div>
        </div>
      `,
      iconSize: [32, 32],
      iconAnchor: [16, 16],
      popupAnchor: [0, -16],
    });
  }, []);

  useEffect(() => {
    if (!mapRef.current || leafletMapRef.current) return;

    let mapInstance: LeafletMap;
    let markersLayer: LayerGroup;

    // Load Leaflet dynamically
    import("leaflet").then((L) => {
      const defaultCenter: [number, number] = center || [-1.2921, 36.8219]; // Default Nairobi
      mapInstance = L.map(mapRef.current!).setView(defaultCenter, zoom);

      L.tileLayer("https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      }).addTo(mapInstance);

      markersLayer = L.layerGroup().addTo(mapInstance);

      leafletMapRef.current = mapInstance;
      markersLayerRef.current = markersLayer;

      drawMarkers(L);
    });

    return () => {
      if (mapInstance) {
        mapInstance.remove();
        leafletMapRef.current = null;
        markersLayerRef.current = null;
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const drawMarkers = useCallback((L: typeof import("leaflet")) => {
    if (!markersLayerRef.current || !leafletMapRef.current) return;

    markersLayerRef.current.clearLayers();

    vendors.forEach((vendor) => {
      if (vendor.latitude === undefined || vendor.longitude === undefined) return;

      const markerName = vendor.name || vendor.businessName || "Vendor";
      const marker = L.marker([vendor.latitude, vendor.longitude], {
        icon: createStoreIcon(L, markerName),
      });

      const popupContent = `
        <div class="p-1 font-sans text-xs">
          <h4 class="font-bold text-slate-800 text-sm mb-1">${markerName}</h4>
          <p class="text-slate-600 mb-0.5">${vendor.foodType || ""}</p>
          <div class="flex items-center gap-1 text-amber-600 font-semibold mt-1">
            <span>★</span>
            <span>${vendor.rating ? vendor.rating.toFixed(1) : "N/A"}</span>
          </div>
          <div class="mt-2.5">
            <a href="/vendors/${vendor.id}" style="color: white; background-color: #2563EB; padding: 4px 8px; border-radius: 4px; font-weight: 600; text-decoration: none; display: inline-block;">
              View Menu
            </a>
          </div>
        </div>
      `;

      marker.bindPopup(popupContent);
      markersLayerRef.current?.addLayer(marker);
    });

    if (vendors.length > 0 && fitBounds) {
      const bounds = vendors
        .filter((v) => v.latitude !== undefined && v.longitude !== undefined)
        .map((v) => [v.latitude, v.longitude] as [number, number]);

      if (bounds.length > 0) {
        leafletMapRef.current?.fitBounds(bounds, { padding: [40, 40] });
      }
    } else if (center) {
      leafletMapRef.current?.setView(center, zoom);
    }
  }, [vendors, center, zoom, fitBounds, createStoreIcon]);

  useEffect(() => {
    if (leafletMapRef.current) {
      import("leaflet").then((L) => {
        drawMarkers(L);
      });
    }
  }, [vendors, center, zoom, fitBounds, drawMarkers]);

  return (
    <div className="relative w-full h-full bg-slate-100 rounded-xl overflow-hidden border border-border">
      <div ref={mapRef} className={className} style={{ zIndex: 1 }} />
    </div>
  );
}
