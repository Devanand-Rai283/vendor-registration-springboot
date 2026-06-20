"use client";

import React, { Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { Search as SearchIcon, UtensilsCrossed, SlidersHorizontal } from "lucide-react";
import { Container } from "@/components/layout/Container";
import { PageHeader } from "@/components/layout/PageHeader";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { FoodSearchCard } from "@/components/cards/FoodSearchCard";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { Pagination } from "@/components/ui/pagination";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { Card } from "@/components/ui/card";
import { useFoodSearch } from "@/features/vendors/hooks/useVendorQueries";

function SearchContent() {
  const searchParams = useSearchParams();
  const router = useRouter();

  // Read strictly from URL to ensure bookmarkability and correct back/forward navigation
  const initialQuery = searchParams.get("q") || "";
  const foodType = searchParams.get("foodType") || "All";
  const dietaryTag = searchParams.get("dietaryTag") || "All";
  const pageParam = parseInt(searchParams.get("page") || "1", 10);
  const currentPage = isNaN(pageParam) ? 1 : Math.max(1, pageParam);

  // Local state for the search input so user can type before hitting Enter
  const [queryInput, setQueryInput] = React.useState(initialQuery);
  const [showFilters, setShowFilters] = React.useState(false);

  // Sync input if URL changes externally (e.g., browser back button)
  React.useEffect(() => {
    setQueryInput(initialQuery);
  }, [initialQuery]);

  // Hook handles API logic. Page is 0-indexed on backend.
  const { data, isLoading, error, refetch } = useFoodSearch({
    q: initialQuery,
    foodType: foodType !== "All" ? foodType : undefined,
    dietaryTag: dietaryTag !== "All" ? dietaryTag : undefined,
    page: currentPage - 1,
    size: 12,
  });

  const hasSearched = !!initialQuery;

  const updateSearch = (newQuery: string, newFoodType: string, newDietaryTag: string, newPage: number) => {
    const params = new URLSearchParams();
    if (newQuery.trim()) params.set("q", newQuery.trim());
    if (newFoodType !== "All") params.set("foodType", newFoodType);
    if (newDietaryTag !== "All") params.set("dietaryTag", newDietaryTag);
    if (newPage > 1) params.set("page", String(newPage));
    router.push(`/search?${params.toString()}`);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (queryInput.trim()) {
      updateSearch(queryInput, foodType, dietaryTag, 1);
    }
  };

  const handleFoodTypeChange = (type: string) => {
    updateSearch(initialQuery, type, dietaryTag, 1);
  };

  const handleDietaryTagChange = (tag: string) => {
    updateSearch(initialQuery, foodType, tag, 1);
  };

  const handlePageChange = (page: number) => {
    updateSearch(initialQuery, foodType, dietaryTag, page);
    // Scroll to top of results smoothly when paginating
    if (typeof window !== "undefined") {
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Container className="py-8">
        <PageHeader
          title="Search Food & Vendors"
          description="Find your favorite dishes and local vendors"
        />

        <div className="flex flex-col gap-4 mb-8">
          <form onSubmit={handleSearchSubmit} className="flex flex-col sm:flex-row gap-3">
            <div className="relative flex-1">
              <SearchIcon className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-text-secondary" />
              <Input
                type="text"
                placeholder="Search for food, cuisines, or ingredients..."
                value={queryInput}
                onChange={(e) => setQueryInput(e.target.value)}
                className="pl-9 h-11"
                aria-label="Search food and vendors"
              />
            </div>
            <div className="flex gap-2 shrink-0">
              <Button type="submit" variant="primary" className="h-11 px-6 flex-1 sm:flex-none">
                Search
              </Button>
              <Button
                type="button"
                variant={showFilters ? "primary" : "outline"}
                className="h-11 px-4"
                onClick={() => setShowFilters(!showFilters)}
              >
                <SlidersHorizontal className="h-4 w-4" />
              </Button>
            </div>
          </form>

          {showFilters && (
            <Card className="p-4 border-border bg-surface shadow-sm animate-in fade-in slide-in-from-top-2 duration-150">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                <div>
                  <label className="block text-xs font-semibold text-text-secondary uppercase tracking-wider mb-2">
                    Food Type
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {["All", "Vegetarian", "Non-Vegetarian"].map((type) => (
                      <Button
                        key={type}
                        variant={foodType === type ? "primary" : "outline"}
                        size="sm"
                        onClick={() => handleFoodTypeChange(type)}
                      >
                        {type}
                      </Button>
                    ))}
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-semibold text-text-secondary uppercase tracking-wider mb-2">
                    Dietary Preference
                  </label>
                  <div className="flex flex-wrap gap-2">
                    {["All", "Vegetarian", "Vegan", "Gluten-Free", "Halal"].map((tag) => (
                      <Button
                        key={tag}
                        variant={dietaryTag === tag ? "primary" : "outline"}
                        size="sm"
                        onClick={() => handleDietaryTagChange(tag)}
                      >
                        {tag}
                      </Button>
                    ))}
                  </div>
                </div>
              </div>
            </Card>
          )}
        </div>

        {!hasSearched ? (
          <EmptyState
            title="Search for something"
            description="Enter a search term above to find delicious food from approved vendors."
            icon={<SearchIcon className="h-6 w-6" />}
          />
        ) : isLoading ? (
          <LoadingState message="Searching for delicious food..." />
        ) : error ? (
          <ErrorState error={error} title="Search failed" onRetry={refetch} />
        ) : !data?.content || data.content.length === 0 ? (
          <EmptyState
            title="No results found"
            description={`We couldn't find any items matching "${initialQuery}". Try adjusting your filters or search term.`}
            icon={<UtensilsCrossed className="h-6 w-6" />}
          />
        ) : (
          <div className="space-y-8 animate-in fade-in duration-300">
            <section>
              <SectionHeader
                title="Search Results"
                subtitle={`Found ${data.totalElements} matching items`}
              />
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
                {data.content.map((item) => (
                  <FoodSearchCard
                    key={`${item.vendorId}-${item.menuItemId}`}
                    menuItemId={item.menuItemId}
                    itemName={item.itemName}
                    description={item.description}
                    price={item.price}
                    dietaryTag={item.dietaryTag}
                    vendorId={item.vendorId}
                    vendorName={item.vendorName}
                    foodType={item.foodType}
                    averageRating={item.averageRating}
                  />
                ))}
              </div>
            </section>

            {data.totalPages > 1 && (
              <div className="pt-6 border-t border-border/50">
                <Pagination
                  currentPage={currentPage}
                  totalPages={data.totalPages}
                  onPageChange={handlePageChange}
                />
              </div>
            )}
          </div>
        )}
      </Container>
    </div>
  );
}

export default function SearchPage() {
  return (
    <Suspense fallback={<LoadingState message="Loading search..." />}>
      <SearchContent />
    </Suspense>
  );
}
