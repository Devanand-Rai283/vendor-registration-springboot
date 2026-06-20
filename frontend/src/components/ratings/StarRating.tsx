'use client';

import React, { useState } from 'react';
import { Star } from 'lucide-react';
import { cn } from '@/lib/utils';

interface StarRatingProps {
  value: number;
  readonly?: boolean;
  onChange?: (value: number) => void;
  className?: string;
  size?: number;
}

export function StarRating({
  value,
  readonly = false,
  onChange,
  className,
  size = 24,
}: StarRatingProps) {
  const [hoverValue, setHoverValue] = useState<number | null>(null);

  const displayValue = hoverValue !== null ? hoverValue : value;

  const handleKeyDown = (e: React.KeyboardEvent<HTMLButtonElement>, starIndex: number) => {
    if (readonly) return;
    
    if (e.key === 'Enter' || e.key === ' ') {
      e.preventDefault();
      onChange?.(starIndex);
    } else if (e.key === 'ArrowRight') {
      e.preventDefault();
      const nextValue = Math.min(5, starIndex + 1);
      onChange?.(nextValue);
    } else if (e.key === 'ArrowLeft') {
      e.preventDefault();
      const prevValue = Math.max(1, starIndex - 1);
      onChange?.(prevValue);
    }
  };

  return (
    <div
      className={cn("flex items-center gap-1", className)}
      role={readonly ? "img" : "radiogroup"}
      aria-label={readonly ? `${value} out of 5 stars` : "Rate 1 to 5 stars"}
    >
      {[1, 2, 3, 4, 5].map((starIndex) => {
        const isFilled = starIndex <= Math.round(displayValue);
        const isHalf = !isFilled && starIndex - 0.5 <= displayValue;

        return (
          <button
            key={starIndex}
            type="button"
            disabled={readonly}
            onClick={() => !readonly && onChange?.(starIndex)}
            onMouseEnter={() => !readonly && setHoverValue(starIndex)}
            onMouseLeave={() => !readonly && setHoverValue(null)}
            onKeyDown={(e) => handleKeyDown(e, starIndex)}
            className={cn(
              "p-0.5 rounded-sm focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary focus-visible:ring-offset-2",
              readonly ? "cursor-default" : "cursor-pointer transition-transform hover:scale-110 active:scale-95"
            )}
            role={readonly ? "presentation" : "radio"}
            aria-checked={!readonly ? value === starIndex : undefined}
            aria-label={`${starIndex} star${starIndex > 1 ? 's' : ''}`}
            tabIndex={readonly ? -1 : (value === starIndex || (value === 0 && starIndex === 1)) ? 0 : -1}
          >
            <Star
              size={size}
              className={cn(
                "transition-colors",
                isFilled || isHalf
                  ? "fill-yellow-400 text-yellow-400"
                  : "text-gray-300 dark:text-gray-600",
                isHalf && "fill-yellow-400/50" // simple approximation for half-stars if needed in read-only
              )}
            />
          </button>
        );
      })}
    </div>
  );
}
