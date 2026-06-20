import React from 'react';
import { ShoppingBag } from 'lucide-react';

export function EmptyCart() {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-4 text-center">
      <div className="h-12 w-12 rounded-full bg-gray-100 flex items-center justify-center mb-4">
        <ShoppingBag className="h-6 w-6 text-gray-400" />
      </div>
      <h3 className="text-lg font-medium text-gray-900 mb-1">Your cart is empty</h3>
      <p className="text-sm text-gray-500 max-w-xs mx-auto">
        Looks like you haven&apos;t added anything to your cart yet. Browse vendors to find something delicious!
      </p>
    </div>
  );
}
