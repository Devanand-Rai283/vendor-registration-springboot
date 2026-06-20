import { apiClient as api } from '@/services/api/apiClient';
import type { ApiResponse } from '@/services/api/types';
import type {
  VendorDashboardMetrics,
  VendorProfile,
  UpdateVendorProfileRequest,
  VendorOrderSummary,
  VendorOrderDetail,
  VendorDocument,
  AnalyticsResponse,
  Page
} from '../types';

async function unwrapApiResponse<T>(request: Promise<ApiResponse<T>>): Promise<T> {
  const response = await request;
  return response.data;
}

export const vendorPortalApi = {
  getDashboardMetrics: () =>
    unwrapApiResponse(api.get<ApiResponse<VendorDashboardMetrics>>('/vendors/dashboard/metrics')),

  getVendorProfile: () =>
    unwrapApiResponse(api.get<ApiResponse<VendorProfile>>('/vendors/me/profile')),

  updateVendorProfile: (data: UpdateVendorProfileRequest) =>
    unwrapApiResponse(api.put<ApiResponse<VendorProfile>>('/vendors/me/profile', data)),

  getVendorOrders: (page = 0, size = 20, status?: string) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    });
    if (status) {
      params.append('status', status);
    }
    return unwrapApiResponse(api.get<ApiResponse<Page<VendorOrderSummary>>>(`/vendors/orders?${params.toString()}`));
  },

  getVendorOrderDetails: (id: string) =>
    unwrapApiResponse(api.get<ApiResponse<VendorOrderDetail>>(`/vendors/orders/${id}`)),

  getVendorDocuments: () =>
    unwrapApiResponse(api.get<ApiResponse<VendorDocument[]>>('/vendors/documents')),

  getVendorAnalytics: (id: string, days = 30) =>
    // Note: AnalyticsController may not use ApiResponse wrapper, based on verification report
    // "Returns AnalyticsResponseDto directly" (wait, my verification report said it supports it directly)
    // Actually in AnalyticsController it says `return ResponseEntity.ok(response);` without ApiResponse.
    // Let me check my verification report. Wait, for Item 6 I didn't explicitly check if it's wrapped.
    // Let me double check AnalyticsController. "return ResponseEntity.ok(response);" Yes, it's NOT wrapped in ApiResponse!
    api.get<AnalyticsResponse>(`/vendors/${id}/analytics?days=${days}`),
};
