import { apiClient } from "@/services/api/apiClient";
import { ApiResponse } from "@/services/api/types";
import { AdminDashboardData, AdminVendorSummary, AdminVendorDetail, PageResponse, VendorStatus } from "../types";

export const adminApi = {
  getDashboardMetrics: async (): Promise<ApiResponse<AdminDashboardData>> => {
    return apiClient.get<ApiResponse<AdminDashboardData>>("/api/admin/dashboard");
  },

  getVendors: async (
    page: number,
    size: number,
    status?: VendorStatus
  ): Promise<ApiResponse<PageResponse<AdminVendorSummary>>> => {
    const params = new URLSearchParams();
    params.append("page", page.toString());
    params.append("size", size.toString());
    if (status) {
      params.append("status", status);
    }
    return apiClient.get<ApiResponse<PageResponse<AdminVendorSummary>>>(
      `/api/admin/vendors?${params.toString()}`
    );
  },

  getVendorDetails: async (id: string): Promise<ApiResponse<AdminVendorDetail>> => {
    return apiClient.get<ApiResponse<AdminVendorDetail>>(`/api/admin/vendors/${id}`);
  },

  suspendVendor: async (id: string): Promise<ApiResponse<void>> => {
    return apiClient.post<ApiResponse<void>>(`/api/admin/vendors/${id}/suspend`);
  },

  reactivateVendor: async (id: string): Promise<ApiResponse<void>> => {
    return apiClient.post<ApiResponse<void>>(`/api/admin/vendors/${id}/reactivate`);
  },
};
