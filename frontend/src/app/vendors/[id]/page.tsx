"use client";

import React from "react";
import { useParams } from "next/navigation";
import { Star, MapPin, ChevronLeft } from "lucide-react";
import Link from "next/link";
import { Container } from "@/components/layout/Container";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { MenuItemCard } from "@/components/cards/MenuItemCard";
import { VendorReviewsList } from "@/features/ratings/components/VendorReviewsList";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { useVendorDetails, useVendorMenu } from "@/features/vendors/hooks/useVendorQueries";
import type { ApiError } from "@/services/api/types";
import { useCartStore } from "@/store/cartStore";
import { CartDrawer } from "@/components/cart/CartDrawer";
import { ShoppingCart } from "lucide-react";

export default function VendorDetailPage() {
  const { id } = useParams() as { id: string };
  const [isCartOpen, setIsCartOpen] = React.useState(false);
  const { itemCount } = useCartStore();

  const {
    data: vendor,
    isLoading: detailsLoading,
    error: detailsError,
    refetch: refetchDetails,
  } = useVendorDetails(id);

  const {
    data: menuData,
    isLoading: menuLoading,
    error: menuError,
    refetch: refetchMenu,
  } = useVendorMenu(id);

  const loading = detailsLoading || menuLoading;
  const error = detailsError || menuError;

  const handleRetry = () => {
    refetchDetails();
    refetchMenu();
  };

  const is404 = 
    (detailsError as unknown as ApiError)?.status === 404 || 
    (menuError as unknown as ApiError)?.status === 404;

  if (loading) {
    return (
      <Container className="py-8">
        <LoadingState message="Loading vendor details & menu..." />
      </Container>
    );
  }

  if (is404) {
    return (
      <Container className="py-8">
        <EmptyState title="Vendor not found" description="The vendor you're looking for doesn't exist or is not approved." />
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="py-8">
        <ErrorState error={error} title="Failed to load vendor details" onRetry={handleRetry} />
      </Container>
    );
  }

  if (!vendor) {
    return (
      <Container className="py-8">
        <EmptyState title="Vendor not found" description="The vendor you're looking for doesn't exist or is not approved." />
      </Container>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Container className="py-8">
        {/* Back link */}
        <Link
          href="/vendors"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-street-blue transition-colors mb-6"
        >
          <ChevronLeft className="h-4 w-4" /> Back to Vendors
        </Link>

        {/* Vendor Header Information */}
        <div className="p-6 bg-surface border border-border rounded-2xl mb-8 shadow-sm">
          <div className="flex flex-col md:flex-row items-start gap-6">
            <div className="relative h-20 w-20 shrink-0 rounded-xl overflow-hidden bg-gradient-to-br from-street-blue/10 to-vendor-orange/10 flex items-center justify-center border border-border">
              <MapPin className="h-8 w-8 text-street-blue" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex items-center gap-3 flex-wrap">
                <h1 className="text-2xl font-bold text-text-primary">{vendor.businessName}</h1>
                <span className="inline-flex items-center rounded-full bg-success/15 px-2.5 py-0.5 text-xs font-semibold text-success border border-success/20">
                  Approved
                </span>
              </div>
              <p className="mt-1 text-sm text-text-secondary font-medium">{vendor.foodType}</p>
              <div className="mt-2.5 flex items-center gap-4 text-sm text-text-secondary">
                <span className="inline-flex items-center gap-1 text-amber-600 font-semibold">
                  <Star className="h-4 w-4 fill-current" /> {vendor.averageRating ? vendor.averageRating.toFixed(1) : "0.0"}
                </span>
                <span className="inline-flex items-center gap-1">
                  <MapPin className="h-4 w-4 text-text-secondary" /> {vendor.address}
                </span>
              </div>
              {vendor.description && (
                <p className="mt-4 text-sm text-text-secondary leading-relaxed border-t border-border/60 pt-3">
                  {vendor.description}
                </p>
              )}
            </div>
          </div>
        </div>

        {/* Menu Categories and Items */}
        <section className="mb-12">
          <SectionHeader
            title="Menu"
            subtitle="Browse available items"
          />

          {!menuData?.categories || menuData.categories.length === 0 ? (
            <EmptyState
              title="No items available"
              description="This vendor hasn't listed any available menu items yet."
            />
          ) : (
            <div className="space-y-8">
              {menuData.categories.map((category) => (
                <div key={category.id} className="bg-surface p-6 border border-border rounded-xl shadow-sm">
                  <h3 className="font-bold text-text-primary text-base mb-4 border-b border-border/80 pb-2">
                    {category.name}
                  </h3>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                    {category.items.map((item) => (
                      <MenuItemCard
                        key={item.id}
                        id={item.id}
                        vendorId={id}
                        name={item.name}
                        description={item.description}
                        price={item.price}
                        available={item.available}
                        imageUrl={item.imageUrl}
                        currency="KES"
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Ratings & Reviews */}
        <section>
          <SectionHeader title="Ratings & Reviews" subtitle="Customer feedback" />
          <div className="mb-6 flex items-center gap-2 px-1">
            <Star className="h-6 w-6 fill-amber-500 text-amber-500" />
            <span className="text-xl font-bold text-text-primary">
              {vendor.averageRating ? vendor.averageRating.toFixed(1) : "0.0"} Rating
            </span>
          </div>
          <VendorReviewsList vendorId={id} />
        </section>
      </Container>
      
      {/* Floating Cart Button for Mobile/Convenience */}
      {itemCount > 0 && (
        <button
          onClick={() => setIsCartOpen(true)}
          className="fixed bottom-6 right-6 z-40 bg-street-blue text-white rounded-full p-4 shadow-lg flex items-center justify-center hover:bg-street-blue/90 transition-transform hover:scale-105"
        >
          <ShoppingCart className="h-6 w-6" />
          <span className="absolute -top-2 -right-2 bg-vendor-orange text-white text-xs font-bold w-6 h-6 flex items-center justify-center rounded-full border-2 border-white">
            {itemCount}
          </span>
        </button>
      )}

      <CartDrawer isOpen={isCartOpen} onClose={() => setIsCartOpen(false)} />
    </div>
  );
}
