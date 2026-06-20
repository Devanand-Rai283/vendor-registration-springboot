import React from 'react';
import { useCartStore } from '@/store/cartStore';

export function CartSummary() {
  const { subtotal } = useCartStore();
  const tax = subtotal * 0.05; // 5% GST for example
  const total = subtotal + tax;

  return (
    <div className="space-y-4">
      <div className="space-y-2 text-sm">
        <div className="flex justify-between text-gray-500">
          <span>Subtotal</span>
          <span>₹{subtotal.toFixed(2)}</span>
        </div>
        <div className="flex justify-between text-gray-500">
          <span>Taxes (5%)</span>
          <span>₹{tax.toFixed(2)}</span>
        </div>
        <div className="flex justify-between font-medium text-gray-900 pt-2 border-t">
          <span>Total</span>
          <span>₹{total.toFixed(2)}</span>
        </div>
      </div>
    </div>
  );
}
