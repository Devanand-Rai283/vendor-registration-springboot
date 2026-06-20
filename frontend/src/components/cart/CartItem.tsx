import React from 'react';
import { Minus, Plus, Trash2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useCartStore } from '@/store/cartStore';

interface CartItemProps {
  menuItemId: string;
  name: string;
  price: number;
  quantity: number;
}

export function CartItem({ menuItemId, name, price, quantity }: CartItemProps) {
  const { updateQuantity, removeItem } = useCartStore();

  return (
    <div className="flex items-center justify-between py-4 border-b">
      <div className="flex-1 min-w-0 pr-4">
        <h4 className="text-sm font-medium text-gray-900 truncate">{name}</h4>
        <div className="mt-1 flex items-center text-sm text-gray-500">
          <span>₹{price.toFixed(2)}</span>
        </div>
      </div>
      
      <div className="flex items-center gap-3">
        <div className="flex items-center border rounded-md">
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 rounded-none rounded-l-md"
            onClick={() => updateQuantity(menuItemId, quantity - 1)}
          >
            <Minus className="h-3 w-3" />
            <span className="sr-only">Decrease quantity</span>
          </Button>
          <span className="w-8 text-center text-sm font-medium">{quantity}</span>
          <Button
            variant="ghost"
            size="icon"
            className="h-8 w-8 rounded-none rounded-r-md"
            onClick={() => updateQuantity(menuItemId, quantity + 1)}
          >
            <Plus className="h-3 w-3" />
            <span className="sr-only">Increase quantity</span>
          </Button>
        </div>
        
        <Button
          variant="ghost"
          size="icon"
          className="h-8 w-8 text-red-500 hover:text-red-600 hover:bg-red-50"
          onClick={() => removeItem(menuItemId)}
        >
          <Trash2 className="h-4 w-4" />
          <span className="sr-only">Remove item</span>
        </Button>
      </div>
    </div>
  );
}
