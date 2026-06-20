import { useMutation, useQuery } from '@tanstack/react-query';
import { paymentApi, CreatePaymentOrderRequest } from '../api/paymentApi';

export const useCreatePaymentOrder = () => {
  return useMutation({
    mutationFn: (request: CreatePaymentOrderRequest) => paymentApi.createPaymentOrder(request),
  });
};

export const useVerifyPayment = (orderId: string, enabled: boolean = true) => {
  return useQuery({
    queryKey: ['payment-verification', orderId],
    queryFn: () => paymentApi.verifyPaymentStatus(orderId),
    enabled: !!orderId && enabled,
    refetchInterval: (query) => {
      // Keep polling every 3 seconds if status is not PAID, FAILED, or REFUNDED
      const data = query.state.data;
      if (!data) return 3000;
      if (['PAID', 'FAILED', 'REFUNDED'].includes(data.paymentStatus)) {
        return false; // Stop polling
      }
      return 3000;
    },
    refetchIntervalInBackground: true,
  });
};
