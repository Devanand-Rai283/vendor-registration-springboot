'use client';

import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { getVendorReviews } from '@/features/vendors/api/vendorApi';
import { StarRating } from '@/components/ratings/StarRating';
import { Pagination } from '@/components/ui/pagination';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/ui/error-state';
import { EmptyState } from '@/components/ui/empty-state';
import { MessageSquare } from 'lucide-react';

interface VendorReviewsListProps {
  vendorId: string;
}

export function VendorReviewsList({ vendorId }: VendorReviewsListProps) {
  const [page, setPage] = useState(0);
  const size = 5;

  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: ['vendor-reviews', vendorId, page, size],
    queryFn: () => getVendorReviews(vendorId, page, size),
  });

  if (isLoading) {
    return (
      <div className="space-y-4 mt-8">
        <Skeleton className="h-8 w-48 mb-6" />
        {[...Array(3)].map((_, i) => (
          <div key={i} className="p-4 border rounded-lg space-y-3">
            <div className="flex justify-between items-center">
              <Skeleton className="h-5 w-32" />
              <Skeleton className="h-4 w-24" />
            </div>
            <Skeleton className="h-4 w-24" />
            <Skeleton className="h-16 w-full" />
          </div>
        ))}
      </div>
    );
  }

  if (isError) {
    return (
      <div className="mt-8">
        <ErrorState 
          title="Failed to load reviews" 
          error={error}
          onRetry={() => refetch()}
        />
      </div>
    );
  }

  if (!data || data.content.length === 0) {
    return (
      <div className="mt-8">
        <EmptyState 
          icon={<MessageSquare className="h-6 w-6" />}
          title="No reviews yet"
          description="Be the first customer to leave feedback."
        />
      </div>
    );
  }

  return (
    <div className="mt-10 space-y-6">
      <div className="space-y-4">
        {data.content.map((review) => (
          <div key={review.id} className="p-5 border border-border rounded-xl bg-surface/50">
            <div className="flex justify-between items-start mb-2">
              <div className="font-medium text-text-primary">
                {review.customerDisplayName}
              </div>
              <div className="text-sm text-text-secondary">
                {new Date(review.createdAt).toLocaleDateString(undefined, { 
                  year: 'numeric', 
                  month: 'short', 
                  day: 'numeric' 
                })}
              </div>
            </div>
            <StarRating value={review.stars} readonly size={16} className="mb-3" />
            {review.reviewText && (
              <p className="text-text-secondary text-sm whitespace-pre-wrap">
                {review.reviewText}
              </p>
            )}
          </div>
        ))}
      </div>

      {data.totalPages > 1 && (
        <div className="pt-4 flex justify-center">
          <Pagination
            currentPage={data.pageNumber}
            totalPages={data.totalPages}
            onPageChange={setPage}
          />
        </div>
      )}
    </div>
  );
}
