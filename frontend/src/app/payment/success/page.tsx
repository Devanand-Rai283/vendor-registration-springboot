"use client";

import React, { useEffect, useState, Suspense } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useVerifyPayment } from '@/features/payments/hooks/usePaymentQueries';
import { Container } from '@/components/layout/Container';
import { Button } from '@/components/ui/button';
import { CheckCircle2, Loader2, AlertCircle } from 'lucide-react';

function PaymentSuccessContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const orderId = searchParams.get('orderId');
  
  const [startTime] = useState(Date.now());
  const [isTimeout, setIsTimeout] = useState(false);

  // Poll for verification, stop if timeout or successful
  const { data: verificationData, isError } = useVerifyPayment(orderId || '', !isTimeout);

  useEffect(() => {
    // Check for timeout (60 seconds)
    const interval = setInterval(() => {
      if (Date.now() - startTime > 60000) {
        setIsTimeout(true);
        clearInterval(interval);
      }
    }, 1000);

    return () => clearInterval(interval);
  }, [startTime]);

  if (!orderId) {
    return (
      <Container className="py-20 flex flex-col items-center justify-center text-center">
        <AlertCircle className="h-16 w-16 text-danger mb-4" />
        <h1 className="text-2xl font-bold mb-2">Invalid Request</h1>
        <p className="text-gray-500 mb-6">No order ID provided.</p>
        <Button onClick={() => router.push('/orders')}>Return to Orders</Button>
      </Container>
    );
  }

  const isPaid = verificationData?.paymentStatus === 'PAID';

  if (!isPaid && !isTimeout && !isError) {
    return (
      <Container className="py-20 flex flex-col items-center justify-center text-center min-h-[60vh]">
        <Loader2 className="h-16 w-16 text-street-blue animate-spin mb-6" />
        <h1 className="text-2xl font-bold mb-2">Verifying Payment...</h1>
        <p className="text-gray-500">Please wait while we confirm your payment with the gateway.</p>
      </Container>
    );
  }

  if (isTimeout && !isPaid) {
    return (
      <Container className="py-20 flex flex-col items-center justify-center text-center min-h-[60vh]">
        <AlertCircle className="h-16 w-16 text-warning mb-6" />
        <h1 className="text-2xl font-bold mb-2">Verification Taking Longer Than Expected</h1>
        <p className="text-gray-500 mb-6 max-w-md">
          Your payment might have been successful, but we are still waiting for confirmation from the gateway. 
          Please check your Orders page in a few minutes.
        </p>
        <div className="flex gap-4">
          <Button onClick={() => router.push('/orders')} variant="default">View Orders</Button>
          <Button onClick={() => router.push('/')} variant="outline">Return Home</Button>
        </div>
      </Container>
    );
  }

  if (isError) {
    return (
      <Container className="py-20 flex flex-col items-center justify-center text-center min-h-[60vh]">
        <AlertCircle className="h-16 w-16 text-danger mb-6" />
        <h1 className="text-2xl font-bold mb-2">Verification Failed</h1>
        <p className="text-gray-500 mb-6">We could not verify your payment status at this time.</p>
        <Button onClick={() => router.push('/orders')}>Return to Orders</Button>
      </Container>
    );
  }

  return (
    <Container className="py-20 flex flex-col items-center justify-center text-center min-h-[60vh]">
      <CheckCircle2 className="h-20 w-20 text-success mb-6" />
      <h1 className="text-3xl font-bold mb-2">Payment Successful!</h1>
      <p className="text-gray-500 mb-8 max-w-md">
        Your order has been confirmed and sent to the vendor. Thank you for your purchase!
      </p>

      <div className="bg-white border rounded-xl p-6 w-full max-w-sm mb-8 text-left shadow-sm">
        <div className="flex justify-between items-center mb-3">
          <span className="text-gray-500 text-sm">Order ID</span>
          <span className="font-medium text-sm font-mono">{verificationData?.orderId?.split('-')[0]}</span>
        </div>
        <div className="flex justify-between items-center mb-3">
          <span className="text-gray-500 text-sm">Payment ID</span>
          <span className="font-medium text-sm font-mono">{verificationData?.razorpayPaymentId}</span>
        </div>
        <div className="flex justify-between items-center pt-3 border-t">
          <span className="text-gray-800 font-semibold">Status</span>
          <span className="text-success font-semibold">Paid</span>
        </div>
      </div>

      <div className="flex gap-4">
        <Button onClick={() => router.push('/orders')} variant="default">View Orders</Button>
        <Button onClick={() => router.push('/vendors')} variant="outline">Continue Browsing</Button>
      </div>
    </Container>
  );
}

export default function PaymentSuccessPage() {
  return (
    <Suspense fallback={
      <Container className="py-20 flex justify-center"><div className="animate-pulse w-8 h-8 bg-gray-200 rounded-full" /></Container>
    }>
      <PaymentSuccessContent />
    </Suspense>
  );
}
