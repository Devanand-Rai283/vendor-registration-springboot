import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { orderApi, PlaceOrderRequest } from '../api/orderApi';
import type { ApiError } from '@/services/api/types';

export const orderKeys = {
  all: ['orders'] as const,
  history: (page: number) => [...orderKeys.all, 'history', page] as const,
};

export function useOrderHistory(page = 0, size = 20) {
  return useQuery({
    queryKey: orderKeys.history(page),
    queryFn: () => orderApi.getOrderHistory(page, size),
  });
}

export function usePlaceOrder() {
  const queryClient = useQueryClient();

  return useMutation<
    Awaited<ReturnType<typeof orderApi.placeOrder>>,
    ApiError,
    PlaceOrderRequest
  >({
    mutationFn: (request) => orderApi.placeOrder(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all });
    },
  });
}

export function useCancelOrder() {
  const queryClient = useQueryClient();

  return useMutation<void, ApiError, string>({
    mutationFn: (orderId) => orderApi.cancelOrder(orderId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: orderKeys.all });
    },
  });
}
