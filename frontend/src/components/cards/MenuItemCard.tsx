"use client";

import React, { useState } from "react";
import Image from "next/image";
import { cn } from "@/utils/cn";
import { Card, CardContent } from "@/components/ui/card";
import { StatusChip } from "@/components/ui/status-chip";
import { Button } from "@/components/ui/button";
import { Plus, Minus, ShoppingCart } from "lucide-react";
import { useCartStore } from "@/store/cartStore";
import { Modal } from "@/components/ui/modal";

interface MenuItemCardProps {
  id: string;
  vendorId: string;
  name: string;
  description: string;
  price: number;
  imageUrl?: string;
  available?: boolean;
  currency?: string;
  className?: string;
}

export function MenuItemCard({
  id,
  vendorId,
  name,
  description,
  price,
  imageUrl,
  available = true,
  currency = "KES",
  className,
}: MenuItemCardProps) {
  const { items, addItem, updateQuantity, vendorId: cartVendorId, clearCart } = useCartStore();
  const [showConfirmModal, setShowConfirmModal] = useState(false);

  const cartItem = items.find((item) => item.menuItemId === id);
  const quantity = cartItem?.quantity || 0;

  const handleAddToCart = () => {
    if (cartVendorId && cartVendorId !== vendorId) {
      setShowConfirmModal(true);
      return;
    }
    
    addItem(
      {
        menuItemId: id,
        name,
        price,
        quantity: 1,
      },
      vendorId
    );
  };

  const confirmReplaceCart = () => {
    clearCart();
    addItem(
      {
        menuItemId: id,
        name,
        price,
        quantity: 1,
      },
      vendorId
    );
    setShowConfirmModal(false);
  };

  return (
    <>
      <Card
        className={cn(
          "overflow-hidden transition-all duration-200 hover:shadow-md",
          !available && "opacity-60",
          className
        )}
      >
        <div className="flex">
          {imageUrl && (
            <div className="relative h-28 w-28 shrink-0 overflow-hidden">
              <Image
                src={imageUrl}
                alt={name}
                fill
                className="object-cover"
                sizes="112px"
              />
            </div>
          )}
          <CardContent className="flex-1 p-4 flex flex-col justify-between">
            <div>
              <div className="flex items-start justify-between gap-2">
                <div className="min-w-0 flex-1">
                  <h4 className="font-semibold text-gray-900 text-sm truncate">
                    {name}
                  </h4>
                  <p className="mt-1 text-xs text-gray-500 line-clamp-2">
                    {description}
                  </p>
                </div>
                <span className="shrink-0 text-sm font-bold text-gray-900">
                  {currency} {price.toLocaleString()}
                </span>
              </div>
              <div className="mt-2">
                {available ? (
                  <StatusChip status="SUCCESS" label="Available" />
                ) : (
                  <StatusChip status="DANGER" label="Unavailable" />
                )}
              </div>
            </div>

            <div className="mt-4 flex justify-end">
              {!available ? (
                <Button disabled variant="outline" size="sm">
                  Unavailable
                </Button>
              ) : quantity > 0 ? (
                <div className="flex items-center border rounded-md">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 rounded-none rounded-l-md"
                    onClick={() => updateQuantity(id, quantity - 1)}
                  >
                    <Minus className="h-3 w-3" />
                  </Button>
                  <span className="w-8 text-center text-sm font-medium">{quantity}</span>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="h-8 w-8 rounded-none rounded-r-md"
                    onClick={() => updateQuantity(id, quantity + 1)}
                  >
                    <Plus className="h-3 w-3" />
                  </Button>
                </div>
              ) : (
                <Button size="sm" onClick={handleAddToCart}>
                  <ShoppingCart className="mr-2 h-4 w-4" />
                  Add
                </Button>
              )}
            </div>
          </CardContent>
        </div>
      </Card>

      <Modal
        isOpen={showConfirmModal}
        onClose={() => setShowConfirmModal(false)}
        title="Start a new order?"
        description="Your cart contains items from a different vendor. Would you like to clear the cart and start a new order?"
      >
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="outline" onClick={() => setShowConfirmModal(false)}>
            Cancel
          </Button>
          <Button onClick={confirmReplaceCart}>
            Yes, start new order
          </Button>
        </div>
      </Modal>
    </>
  );
}

export default MenuItemCard;
