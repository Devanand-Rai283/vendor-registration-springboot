"use client";

import React, { useState, useEffect, useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Search, ArrowRight, Store, Sparkles, MapPin } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Container } from "@/components/layout/Container";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { VendorCard } from "@/components/cards/VendorCard";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { Input } from "@/components/ui/input";
import { useNearbyVendors } from "@/features/vendors/hooks/useVendorQueries";

const categories = [
  { name: "Street Food", icon: "🍛", count: 24 },
  { name: "Fast Food", icon: "🍔", count: 18 },
  { name: "Snacks", icon: "🥪", count: 22 },
  { name: "Beverages", icon: "🥤", count: 15 },
  { name: "Desserts", icon: "🍩", count: 12 },
  { name: "Traditional", icon: "🥩", count: 20 },
];

export default function HomePage() {
  const router = useRouter();
  const [searchQuery, setSearchQuery] = useState("");
  const [coords, setCoords] = useState({ latitude: -1.2921, longitude: 36.8219 }); // Nairobi Default

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

  const { data: nearbyData, isLoading: vendorsLoading, error: vendorsError, refetch } = useNearbyVendors({
    latitude: coords.latitude,
    longitude: coords.longitude,
    radius: 10,
    page: 0,
    size: 8,
  });

  const topRatedVendors = useMemo(() => {
    if (!nearbyData?.vendors) return [];
    return [...nearbyData.vendors]
      .sort((a, b) => b.averageRating - a.averageRating)
      .slice(0, 4);
  }, [nearbyData?.vendors]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <header className="sticky top-0 z-50 w-full border-b border-border bg-surface/95 backdrop-blur-sm">
        <Container>
          <div className="flex h-16 items-center justify-between">
            <Link href="/" className="flex items-center gap-2 group">
              <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-street-blue text-white shadow-sm">
                <Store className="h-4 w-4" />
              </div>
              <span className="text-lg font-bold tracking-tight text-text-primary">
                Street<span className="text-vendor-orange">Vendor</span>
              </span>
            </Link>
            <nav className="hidden md:flex items-center gap-1">
              <Link href="/" className="inline-flex items-center gap-2 rounded-lg bg-muted px-3 py-2 text-sm font-medium text-street-blue">
                <CompassIcon /> Discover
              </Link>
              <Link href="/vendors" className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-text-secondary hover:bg-muted hover:text-text-primary transition-colors">
                <StoreIcon /> Vendors
              </Link>
              <Link href="/search" className="inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium text-text-secondary hover:bg-muted hover:text-text-primary transition-colors">
                <SearchIcon /> Search
              </Link>
            </nav>
            <div className="flex items-center gap-3">
              <Link href="/login">
                <Button variant="ghost" size="sm">Sign In</Button>
              </Link>
              <Link href="/register">
                <Button variant="primary" size="sm">Get Started</Button>
              </Link>
            </div>
          </div>
        </Container>
      </header>

      <main>
        <Container className="py-8">
          {/* Search Bar */}
          <section className="relative mb-12 overflow-hidden rounded-2xl bg-gradient-to-br from-street-blue/10 via-background to-vendor-orange/5 border border-border/60 p-8 sm:p-12">
            <h1 className="text-3xl sm:text-4xl font-black tracking-tight text-text-primary leading-tight">
              Discover Local Street Vendors
            </h1>
            <p className="mt-3 text-base text-text-secondary max-w-xl">
              Find the best food stalls, trucks, and artisans near you.
            </p>
            <form onSubmit={handleSearchSubmit} className="mt-6 flex max-w-xl gap-3">
              <div className="relative flex-1">
                <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-secondary" />
                <Input
                  type="text"
                  placeholder="Search for food, vendors, or cuisines..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-9 h-11"
                  aria-label="Search for food or vendors"
                />
              </div>
              <Button type="submit" variant="primary" className="h-11 px-6">
                Search
              </Button>
            </form>
          </section>

          {/* Nearby Vendors */}
          <section className="mb-12">
            <SectionHeader
              title="Nearby Vendors"
              subtitle="Discover vendors in your area"
              action={
                <Link href="/vendors">
                  <Button variant="ghost" size="sm">
                    View All <ArrowRight className="ml-1 h-3 w-3" />
                  </Button>
                </Link>
              }
            />
            {vendorsLoading ? (
              <LoadingState message="Finding nearby vendors..." />
            ) : vendorsError ? (
              <ErrorState error={vendorsError} title="Failed to load nearby vendors" onRetry={refetch} />
            ) : !nearbyData?.vendors || nearbyData.vendors.length === 0 ? (
              <EmptyState
                title="No vendors found nearby"
                description="We couldn't find any approved vendors near your location."
                icon={<MapPin className="h-6 w-6" />}
              />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {nearbyData.vendors.map((v) => (
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
          </section>

          {/* Popular Categories */}
          <section className="mb-12">
            <SectionHeader
              title="Popular Categories"
              subtitle="Browse by food type"
            />
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
              {categories.map((cat) => (
                <Link
                  key={cat.name}
                  href={`/search?foodType=${encodeURIComponent(cat.name)}`}
                  className="group"
                >
                  <Card className="hover:shadow-md transition-all duration-200 hover:-translate-y-0.5">
                    <CardContent className="flex flex-col items-center justify-center p-4 text-center">
                      <span className="text-3xl mb-2">{cat.icon}</span>
                      <h3 className="text-sm font-semibold text-text-primary group-hover:text-street-blue transition-colors">
                        {cat.name}
                      </h3>
                      <p className="text-xs text-text-secondary mt-0.5">
                        Browse food
                      </p>
                    </CardContent>
                  </Card>
                </Link>
              ))}
            </div>
          </section>

          {/* Top Rated Vendors */}
          <section className="mb-12">
            <SectionHeader
              title="Top Rated Vendors"
              subtitle="Highest rated in the community"
            />
            {vendorsLoading ? (
              <LoadingState message="Loading top vendors..." />
            ) : vendorsError ? (
              <ErrorState error={vendorsError} title="Failed to load top rated vendors" onRetry={refetch} />
            ) : topRatedVendors.length === 0 ? (
              <EmptyState
                title="No top rated vendors"
                description="No highly rated vendors available in your area."
                icon={<Sparkles className="h-6 w-6" />}
              />
            ) : (
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
                {topRatedVendors.map((v) => (
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
          </section>
        </Container>
      </main>

      <footer className="border-t border-border bg-surface py-8">
        <Container>
          <div className="flex flex-col md:flex-row items-center justify-between gap-4">
            <div className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-success" />
              <span className="text-sm text-text-secondary font-medium">
                Street Vendor Platform &copy; {new Date().getFullYear()}
              </span>
            </div>
            <div className="flex gap-6 text-sm text-text-secondary">
              <Link href="/vendors" className="hover:text-street-blue transition-colors">Vendors</Link>
              <Link href="/search" className="hover:text-street-blue transition-colors">Search</Link>
            </div>
          </div>
        </Container>
      </footer>
    </div>
  );
}

function CompassIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M9 6.75V15m6-6v8.25m.503 3.498l4.875-2.437c.381-.19.622-.58.622-1.006V4.82c0-.836-.88-1.38-1.628-1.006l-3.869 1.934c-.317.159-.69.159-1.006 0L9.503 3.252a1.125 1.125 0 00-1.006 0L3.622 5.689C3.24 5.88 3 6.27 3 6.695V19.18c0 .836.88 1.38 1.628 1.006l3.869-1.934c.317-.159.69-.159 1.006 0l4.994 2.497c.317.158.69.158 1.006 0z" />
    </svg>
  );
}

function StoreIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M13.5 21v-7.5a.75.75 0 01.75-.75h3a.75.75 0 01.75.75V21m-4.5 0H2.36m11.14 0H18m0 0h3.64m-1.39 0V9.349m-16.5 11.65V9.35m0 0a3.001 3.001 0 003.75-.615A2.993 2.993 0 009.75 9.75c.896 0 1.7-.393 2.25-1.016a2.993 2.993 0 002.25 1.016c.896 0 1.7-.393 2.25-1.016a3.001 3.001 0 003.75.614m-16.5 0a3.004 3.004 0 01-.621-4.72L4.318 3.44A1.5 1.5 0 015.378 3h13.243a1.5 1.5 0 011.06.44l1.19 1.189a3 3 0 01-.621 4.72m-13.5 8.65h3.75a.75.75 0 00.75-.75V13.5a.75.75 0 00-.75-.75H6.75a.75.75 0 00-.75.75v3.75c0 .415.336.75.75.75z" />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" strokeWidth={2} stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" />
    </svg>
  );
}
