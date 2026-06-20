"use client";

import React, { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useCartStore } from '@/store/cartStore';
import { usePlaceOrder } from '@/features/orders/hooks/useOrderQueries';
import { useCreatePaymentOrder } from '@/features/payments/hooks/usePaymentQueries';
import { openRazorpayCheckout } from '@/lib/razorpay';
import { Container } from '@/components/layout/Container';
import { SectionHeader } from '@/components/layout/SectionHeader';
import { CartItem } from '@/components/cart/CartItem';
import { CartSummary } from '@/components/cart/CartSummary';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { useToast } from '@/components/ui/toast';
import { EmptyState } from '@/components/ui/empty-state';

export default function CheckoutPage() {
  const router = useRouter();
  const { addToast } = useToast();
  const { items, notes, setNotes, clearCart } = useCartStore();
  const { mutate: placeOrder, isPending: isPlacingOrder } = usePlaceOrder();
  const { mutateAsync: createPaymentOrder, isPending: isCreatingPayment } = useCreatePaymentOrder();
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);
  
  // Local state to prevent hydration mismatches if Zustand initialized after render
  const [mounted, setMounted] = useState(false);
  React.useEffect(() => setMounted(true), []);

  if (!mounted) return null;

  if (items.length === 0) {
    return (
      <Container className="py-12">
        <EmptyState 
          title="Your cart is empty" 
          description="You need to add items to your cart before checking out."
          actionLabel="Browse Vendors"
          onAction={() => router.push('/vendors')}
        />
      </Container>
    );
  }

  const handlePlaceOrder = () => {
    const orderItems = items.map(item => ({
      menuItemId: item.menuItemId,
      quantity: item.quantity
    }));

    placeOrder({ items: orderItems, notes }, {
      onSuccess: async (orderResponse) => {
        try {
          setIsProcessingPayment(true);
          // 1. Create payment order on backend
          const paymentData = await createPaymentOrder({ orderId: orderResponse.orderId });
          
          // 2. Launch Razorpay Checkout
          await openRazorpayCheckout({
            amount: paymentData.amount,
            currency: paymentData.currency,
            order_id: paymentData.razorpayOrderId,
            name: "Street Vendor Platform",
            description: "Order Payment",
          });

          // 3. On success callback, redirect to success page
          clearCart();
          router.push(`/payment/success?orderId=${orderResponse.orderId}`);
        } catch (error: unknown) {
          const err = error as Error;
          // If payment creation fails or user closes/fails Razorpay modal
          clearCart();
          router.push(`/payment/failure?orderId=${orderResponse.orderId}&error=${encodeURIComponent(err.message)}`);
        } finally {
          setIsProcessingPayment(false);
        }
      },
      onError: (error) => {
        addToast({
          title: "Failed to place order",
          description: error.message || "An unexpected error occurred.",
          type: "error",
        });
      }
    });
  };

  const isBusy = isPlacingOrder || isCreatingPayment || isProcessingPayment;

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <Container>
        <SectionHeader title="Checkout" subtitle="Review and place your order" />
        
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 mt-8">
          <div className="lg:col-span-2 space-y-6">
            <div className="bg-white p-6 rounded-xl border shadow-sm">
              <h3 className="text-lg font-bold mb-4">Order Items</h3>
              <div className="space-y-2">
                {items.map((item) => (
                  <CartItem key={item.menuItemId} {...item} />
                ))}
              </div>
            </div>

            <div className="bg-white p-6 rounded-xl border shadow-sm">
              <h3 className="text-lg font-bold mb-4">Additional Notes</h3>
              <Textarea 
                placeholder="Any special instructions for the vendor? (e.g. Extra spicy, no onions)"
                value={notes}
                onChange={(e) => setNotes(e.target.value)}
                rows={3}
              />
            </div>
          </div>

          <div className="lg:col-span-1">
            <div className="bg-white p-6 rounded-xl border shadow-sm sticky top-24">
              <h3 className="text-lg font-bold mb-4">Order Summary</h3>
              <CartSummary />
              
              <div className="mt-6">
                <Button 
                  className="w-full" 
                  size="lg" 
                  onClick={handlePlaceOrder}
                  disabled={isBusy}
                >
                  {isBusy ? 'Processing...' : 'Place Order & Pay'}
                </Button>
              </div>
              
              <p className="text-xs text-center text-gray-500 mt-4">
                By placing this order, you agree to the vendor&apos;s terms and conditions.
              </p>
            </div>
          </div>
        </div>
      </Container>
    </div>
  );
}
