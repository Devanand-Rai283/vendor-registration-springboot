import { apiClient as api } from '@/services/api/apiClient';
import type { PaginatedResponse } from '@/services/api/types';

export interface OrderItemRequest {
  menuItemId: string;
  quantity: number;
}

export interface PlaceOrderRequest {
  items: OrderItemRequest[];
  notes?: string;
}

export interface PlaceOrderResponse {
  orderId: string;
  status: string;
  totalAmount: number;
  createdAt: string;
}

export interface CustomerOrderHistoryResponse {
  orderId: string;
  vendorId: string;
  vendorBusinessName: string;
  status: string;
  paymentStatus: string;
  totalAmount: number;
  createdAt: string;
}

export const orderApi = {
  placeOrder: async (request: PlaceOrderRequest): Promise<PlaceOrderResponse> => {
    // We add an idempotency key to prevent duplicate orders
    const idempotencyKey = crypto.randomUUID();
    const response = await api.post<PlaceOrderResponse>('/orders', request, {
      headers: {
        'X-Idempotency-Key': idempotencyKey,
      },
    });
    return response;
  },

  getOrderHistory: async (page = 0, size = 20): Promise<PaginatedResponse<CustomerOrderHistoryResponse>> => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    const response = await api.get<PaginatedResponse<CustomerOrderHistoryResponse>>(`/orders?${params.toString()}`);
    return response;
  },

  cancelOrder: async (orderId: string): Promise<void> => {
    const response = await api.put<void>(`/orders/${orderId}/cancel`);
    return response;
  },
};
