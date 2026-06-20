import { apiClient as api } from '@/services/api/apiClient';

export interface CreatePaymentOrderRequest {
  orderId: string;
}

export interface CreatePaymentOrderResponse {
  paymentId: string;
  razorpayOrderId: string;
  amount: number;
  currency: string;
  status: string;
}

export interface PaymentVerificationResponse {
  paymentId: string;
  orderId: string;
  paymentStatus: string;
  orderPaymentStatus: string;
  orderStatus: string;
  razorpayOrderId: string;
  razorpayPaymentId: string;
}

export const paymentApi = {
  createPaymentOrder: async (request: CreatePaymentOrderRequest): Promise<CreatePaymentOrderResponse> => {
    const response = await api.post<CreatePaymentOrderResponse>('/payments/create-order', request);
    return response;
  },

  verifyPaymentStatus: async (orderId: string): Promise<PaymentVerificationResponse> => {
    const response = await api.get<PaymentVerificationResponse>(`/payments/orders/${orderId}/verify`);
    return response;
  },
};
