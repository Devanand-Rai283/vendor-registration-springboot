import { env } from '@/utils/env';

export interface RazorpayOptions {
  key: string;
  amount: number;
  currency: string;
  name?: string;
  description?: string;
  order_id: string;
  handler?: (response: RazorpayResponse) => void;
  prefill?: {
    name?: string;
    email?: string;
    contact?: string;
  };
  notes?: Record<string, string>;
  theme?: {
    color?: string;
  };
}

export interface RazorpayResponse {
  razorpay_payment_id: string;
  razorpay_order_id: string;
  razorpay_signature: string;
}

declare global {
  interface Window {
    Razorpay: new (options: RazorpayOptions) => {
      open: () => void;
      on: (event: string, callback: (response: unknown) => void) => void;
    };
  }
}

/**
 * Dynamically loads the Razorpay checkout SDK script.
 * Prevents multiple injections of the script.
 */
export const loadRazorpayScript = (): Promise<boolean> => {
  return new Promise((resolve) => {
    if (typeof window === 'undefined') {
      return resolve(false);
    }

    if (document.getElementById('razorpay-sdk')) {
      return resolve(true);
    }

    const script = document.createElement('script');
    script.id = 'razorpay-sdk';
    script.src = 'https://checkout.razorpay.com/v1/checkout.js';
    script.onload = () => resolve(true);
    script.onerror = () => resolve(false);

    document.body.appendChild(script);
  });
};

/**
 * Initializes and opens the Razorpay checkout modal.
 */
export const openRazorpayCheckout = async (
  options: Omit<RazorpayOptions, 'key'>
): Promise<RazorpayResponse> => {
  const isLoaded = await loadRazorpayScript();
  if (!isLoaded) {
    throw new Error('Razorpay SDK failed to load. Are you online?');
  }

  return new Promise((resolve, reject) => {
    const rzpOptions: RazorpayOptions = {
      ...options,
      key: env.NEXT_PUBLIC_RAZORPAY_KEY_ID,
      handler: (response: RazorpayResponse) => {
        resolve(response);
      },
    };

    const rzp = new window.Razorpay(rzpOptions);

    rzp.on('payment.failed', (response: unknown) => {
      // Rejecting allows the caller to handle failure transitions
      const errRes = response as { error?: { description?: string } };
      reject(new Error(errRes.error?.description || 'Payment failed'));
    });

    rzp.open();
  });
};
