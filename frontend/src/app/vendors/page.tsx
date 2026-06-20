"use client";

import React, { useState, useEffect, useMemo } from "react";
import { Container } from "@/components/layout/Container";
import { PageHeader } from "@/components/layout/PageHeader";
import { VendorCard } from "@/components/cards/VendorCard";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { Input } from "@/components/ui/input";
import { Search, SlidersHorizontal, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card } from "@/components/ui/card";
import { useNearbyVendors } from "@/features/vendors/hooks/useVendorQueries";
import dynamic from "next/dynamic";

// Dynamically import Leaflet Map Component (no SSR due to browser window dependency)
const VendorMap = dynamic(
  () => import("@/components/maps/VendorMap"),
  {
    ssr: false,
    loading: () => (
      <div className="h-[350px] w-full bg-slate-100 border border-border rounded-xl animate-pulse flex flex-col items-center justify-center gap-2 text-text-secondary">
        <MapPin className="h-6 w-6 animate-bounce" />
        <span className="text-sm font-medium">Initializing Map View...</span>
      </div>
    ),
  }
);

export default function VendorsPage() {
  const [searchQuery, setSearchQuery] = useState("");
  const [radius, setRadius] = useState<number>(5); // Default 5 km
  const [foodTypeFilter, setFoodTypeFilter] = useState<string>("All");
  const [coords, setCoords] = useState({ latitude: -1.2921, longitude: 36.8219 }); // Nairobi Default
  const [showFilters, setShowFilters] = useState(false);

  // Request browser geolocation
  useEffect(() => {
    if (typeof window !== "undefined" && navigator.geolocation) {
      navigator.geolocation.getCurrentPosition(
        (pos) => {
          setCoords({
            latitude: pos.coords.latitude,
            longitude: pos.coords.longitude,
          });
        },
        (err) => {
          console.warn("Geolocation declined or unavailable, using defaults:", err.message);
        }
      );
    }
  }, []);

  const { data: nearbyData, isLoading: loading, error, refetch } = useNearbyVendors({
    latitude: coords.latitude,
    longitude: coords.longitude,
    radius: radius,
    page: 0,
    size: 50, // Retrieve a larger page size so client-side filters work nicely
  });

  // Client-side filtering for search query and food type
  const filteredVendors = useMemo(() => {
    if (!nearbyData?.vendors) return [];
    return nearbyData.vendors.filter((v) => {
      const matchesSearch =
        v.businessName.toLowerCase().includes(searchQuery.toLowerCase()) ||
        v.foodType.toLowerCase().includes(searchQuery.toLowerCase());

      const matchesFoodType =
        foodTypeFilter === "All" ||
        v.foodType.toLowerCase() === foodTypeFilter.toLowerCase();

      return matchesSearch && matchesFoodType;
    });
  }, [nearbyData?.vendors, searchQuery, foodTypeFilter]);

  const mapVendors = useMemo(() => {
    return filteredVendors.map((v) => ({
      id: v.id,
      name: v.businessName,
      latitude: v.latitude,
      longitude: v.longitude,
      foodType: v.foodType,
      rating: v.averageRating,
    }));
  }, [filteredVendors]);

  return (
    <div className="min-h-screen bg-background">
      <Container className="py-8">
        <PageHeader
          title="Discover Vendors"
          description="Find local street food, food trucks, and local artisans near you"
        />

        {/* Search & Filters Controls */}
        <div className="flex flex-col gap-4 mb-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="relative flex-1">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-secondary" />
              <Input
                type="text"
                placeholder="Search vendors or cuisines..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9"
                aria-label="Search vendors"
              />
            </div>
            <div className="flex gap-2 shrink-0">
              <Button
                variant={showFilters ? "primary" : "outline"}
                size="default"
                onClick={() => setShowFilters(!showFilters)}
                className="flex items-center gap-2"
              >
                <SlidersHorizontal className="h-4 w-4" />
                <span>Filters</span>
              </Button>
            </div>
          </div>

          {/* Expandable filters panel */}
          {showFilters && (
            <Card className="p-4 border-border bg-surface shadow-sm animate-in fade-in slide-in-from-top-2 duration-150">
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* Radius Selector */}
                <div>
                  <label htmlFor="radius-select" className="block text-xs font-semibold text-text-secondary uppercase tracking-wider mb-2">
                    Search Radius
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {[1, 3, 5, 10, 20].map((r) => (
                      <Button
                        key={r}
                        variant={radius === r ? "primary" : "outline"}
                        size="sm"
                        onClick={() => setRadius(r)}
                      >
                        {r} km
                      </Button>
                    ))}
                  </div>
                </div>

                {/* Food Type Selector */}
                <div>
                  <label htmlFor="foodtype-select" className="block text-xs font-semibold text-text-secondary uppercase tracking-wider mb-2">
                    Food Type
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {["All", "Vegetarian", "Non-Vegetarian", "Mixed"].map((type) => (
                      <Button
                        key={type}
                        variant={foodTypeFilter === type ? "primary" : "outline"}
                        size="sm"
                        onClick={() => setFoodTypeFilter(type)}
                      >
                        {type}
                      </Button>
                    ))}
                  </div>
                </div>
              </div>
            </Card>
          )}
        </div>

        {/* Leaflet Dynamic Map Integration */}
        <div className="mb-8 overflow-hidden rounded-xl">
          <VendorMap
            vendors={mapVendors}
            center={[coords.latitude, coords.longitude]}
            zoom={13}
            className="h-[350px] w-full"
          />
        </div>

        {/* Vendor Grid Results */}
        <div>
          <h2 className="text-lg font-bold text-text-primary mb-4 flex items-center gap-2">
            <span>Results</span>
            <span className="text-xs font-semibold px-2 py-0.5 rounded-full bg-muted text-text-secondary">
              {filteredVendors.length}
            </span>
          </h2>

          {loading ? (
            <LoadingState message="Loading nearby vendors..." />
          ) : error ? (
            <ErrorState error={error} title="Failed to load vendors" onRetry={refetch} />
          ) : filteredVendors.length === 0 ? (
            <EmptyState
              title="No vendors found"
              description={searchQuery ? `No vendors matching "${searchQuery}" in this radius.` : "No vendors available in your area yet. Try increasing the search radius."}
              icon={<MapPin className="h-6 w-6" />}
            />
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredVendors.map((v) => (
                <VendorCard
                  key={v.id}
                  id={v.id}
                  name={v.businessName}
                  rating={v.averageRating}
                  distance={v.distanceKm}
                  foodType={v.foodType}
                />
              ))}
            </div>
          )}
        </div>
      </Container>
    </div>
  );
}
