import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { vendorPortalApi } from '../api/vendorPortalApi';
import type { UpdateVendorProfileRequest } from '../types';

export const vendorKeys = {
  all: ['vendor'] as const,
  dashboard: () => [...vendorKeys.all, 'dashboard'] as const,
  profile: () => [...vendorKeys.all, 'profile'] as const,
  orders: () => [...vendorKeys.all, 'orders'] as const,
  ordersList: (page: number, size: number, status?: string) => [...vendorKeys.orders(), page, size, status] as const,
  orderDetails: (id: string) => [...vendorKeys.orders(), 'detail', id] as const,
  documents: () => [...vendorKeys.all, 'documents'] as const,
  analytics: (id: string, days: number) => [...vendorKeys.all, 'analytics', id, days] as const,
};

export function useVendorDashboard() {
  return useQuery({
    queryKey: vendorKeys.dashboard(),
    queryFn: vendorPortalApi.getDashboardMetrics,
  });
}

export function useVendorProfile() {
  return useQuery({
    queryKey: vendorKeys.profile(),
    queryFn: vendorPortalApi.getVendorProfile,
  });
}

export function useUpdateVendorProfile() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (data: UpdateVendorProfileRequest) => vendorPortalApi.updateVendorProfile(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: vendorKeys.profile() });
    },
  });
}

export function useVendorOrders(page = 0, size = 20, status?: string) {
  return useQuery({
    queryKey: vendorKeys.ordersList(page, size, status),
    queryFn: () => vendorPortalApi.getVendorOrders(page, size, status),
  });
}

export function useVendorOrderDetails(id: string) {
  return useQuery({
    queryKey: vendorKeys.orderDetails(id),
    queryFn: () => vendorPortalApi.getVendorOrderDetails(id),
    enabled: !!id,
  });
}

export function useVendorDocuments() {
  return useQuery({
    queryKey: vendorKeys.documents(),
    queryFn: vendorPortalApi.getVendorDocuments,
  });
}

export function useVendorAnalytics(vendorId: string | undefined, days = 30) {
  return useQuery({
    queryKey: vendorId ? vendorKeys.analytics(vendorId, days) : [],
    queryFn: () => vendorPortalApi.getVendorAnalytics(vendorId!, days),
    enabled: !!vendorId,
  });
}
