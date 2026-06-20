"use client";

import React, { Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { Container } from '@/components/layout/Container';
import { Button } from '@/components/ui/button';
import { XCircle, AlertCircle } from 'lucide-react';
import { openRazorpayCheckout } from '@/lib/razorpay';
import { useCreatePaymentOrder } from '@/features/payments/hooks/usePaymentQueries';
import { useToast } from '@/components/ui/toast';

function PaymentFailureContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderId = searchParams.get('orderId');
  const errorMsg = searchParams.get('error') || 'Your payment was declined or cancelled.';
  
  const { mutateAsync: createPaymentOrder, isPending: isRetrying } = useCreatePaymentOrder();
  const { addToast } = useToast();

  const handleRetry = async () => {
    if (!orderId) return;
    try {
      const paymentData = await createPaymentOrder({ orderId });
      await openRazorpayCheckout({
        amount: paymentData.amount,
        currency: paymentData.currency,
        order_id: paymentData.razorpayOrderId,
        name: "Street Vendor Platform",
        description: "Order Payment Retry",
      });
      router.push(`/payment/success?orderId=${orderId}`);
    } catch (error: unknown) {
      const err = error as Error;
      addToast({
        title: "Retry failed",
        description: err.message || "Could not launch checkout again.",
        type: "error",
      });
    }
  };

  return (
    <Container className="py-20 flex flex-col items-center justify-center text-center min-h-[60vh]">
      <XCircle className="h-20 w-20 text-danger mb-6" />
      <h1 className="text-3xl font-bold mb-2">Payment Failed</h1>
      <p className="text-gray-500 mb-8 max-w-md">
        {errorMsg}
      </p>

      {orderId && (
        <div className="bg-danger/5 border border-danger/20 rounded-xl p-6 w-full max-w-sm mb-8 text-left shadow-sm">
          <div className="flex items-center gap-2 mb-3">
            <AlertCircle className="h-5 w-5 text-danger" />
            <span className="font-medium text-danger">Action Required</span>
          </div>
          <p className="text-sm text-gray-600">
            Your order has been saved but will not be processed by the vendor until payment is completed.
          </p>
        </div>
      )}

      <div className="flex flex-col sm:flex-row gap-4">
        {orderId && (
          <Button 
            onClick={handleRetry} 
            variant="default"
            disabled={isRetrying}
          >
            {isRetrying ? 'Launching...' : 'Retry Payment'}
          </Button>
        )}
        <Button onClick={() => router.push('/orders')} variant="outline">View Orders</Button>
        <Button onClick={() => router.push('/')} variant="ghost">Return Home</Button>
      </div>
    </Container>
  );
}

export default function PaymentFailurePage() {
  return (
    <Suspense fallback={
      <Container className="py-20 flex justify-center"><div className="animate-pulse w-8 h-8 bg-gray-200 rounded-full" /></Container>
    }>
      <PaymentFailureContent />
    </Suspense>
  );
}
