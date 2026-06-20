'use client';

import React, { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Modal } from '@/components/ui/modal';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { StarRating } from '@/components/ratings/StarRating';
import { ratingApi } from '../api/ratingApi';
import { useToast } from '@/components/ui/toast';
import { ApiError } from '@/services/api/types';

interface ReviewModalProps {
  isOpen: boolean;
  onClose: () => void;
  orderId: string;
  vendorId: string;
}

export function ReviewModal({ isOpen, onClose, orderId, vendorId }: ReviewModalProps) {
  const [stars, setStars] = useState<number>(0);
  const [reviewText, setReviewText] = useState('');
  const [error, setError] = useState<string | null>(null);
  
  const { addToast } = useToast();
  const queryClient = useQueryClient();

  const { mutate: submitReview, isPending } = useMutation({
    mutationFn: () => ratingApi.submitReview({ orderId, stars, reviewText }),
    onSuccess: () => {
      addToast({
        title: 'Review submitted',
        description: 'Your review was submitted successfully.',
        type: 'success'
      });
      // Invalidate relevant queries to refresh data
      queryClient.invalidateQueries({ queryKey: ['vendor-reviews', vendorId] });
      queryClient.invalidateQueries({ queryKey: ['vendor', vendorId] });
      
      // Reset form and close
      handleClose();
    },
    onError: (err: unknown) => {
      const apiErr = err as ApiError;
      if (apiErr.status === 409) {
        setError('You have already reviewed this order.');
      } else {
        setError(apiErr.message || 'Failed to submit review');
      }
    }
  });

  const handleClose = () => {
    setStars(0);
    setReviewText('');
    setError(null);
    onClose();
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    
    if (stars === 0) {
      setError('Please select a star rating');
      return;
    }
    
    submitReview();
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={handleClose}
      title="Rate your order"
      description="Let others know how your experience was."
    >
      <form onSubmit={handleSubmit} className="space-y-6">
        <div className="flex flex-col items-center justify-center space-y-3 pt-4">
          <StarRating 
            value={stars} 
            onChange={setStars} 
            size={36}
            className="mb-2"
          />
          <p className="text-sm font-medium text-text-secondary">
            {stars === 0 ? "Select a rating" : `${stars} star${stars > 1 ? 's' : ''}`}
          </p>
        </div>

        <div className="space-y-2">
          <label htmlFor="reviewText" className="text-sm font-medium text-text-primary">
            Review (Optional)
          </label>
          <Textarea
            id="reviewText"
            placeholder="What did you like or dislike?"
            value={reviewText}
            onChange={(e) => setReviewText(e.target.value)}
            rows={4}
            disabled={isPending}
            className="resize-none"
          />
        </div>

        {error && (
          <div className="text-sm font-medium text-red-500 bg-red-50 dark:bg-red-900/20 p-3 rounded-md">
            {error}
          </div>
        )}

        <div className="flex justify-end space-x-3 pt-2">
          <Button 
            type="button" 
            variant="outline" 
            onClick={handleClose}
            disabled={isPending}
          >
            Cancel
          </Button>
          <Button 
            type="submit" 
            disabled={isPending || stars === 0}
          >
            {isPending ? 'Submitting...' : 'Submit Review'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
