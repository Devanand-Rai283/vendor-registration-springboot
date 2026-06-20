import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { adminApi } from "../api/adminApi";
import { VendorStatus } from "../types";

export function useAdminDashboard() {
  return useQuery({
    queryKey: ["admin", "dashboard"],
    queryFn: async () => {
      const response = await adminApi.getDashboardMetrics();
      return response.data;
    },
  });
}

export function useAdminVendors(page: number, size: number, status?: VendorStatus) {
  return useQuery({
    queryKey: ["admin", "vendors", { page, size, status }],
    queryFn: async () => {
      const response = await adminApi.getVendors(page, size, status);
      return response.data;
    },
  });
}

export function useAdminVendorDetails(id: string) {
  return useQuery({
    queryKey: ["admin", "vendor-details", id],
    queryFn: async () => {
      const response = await adminApi.getVendorDetails(id);
      return response.data;
    },
    enabled: !!id,
  });
}

export function useSuspendVendor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.suspendVendor(id),
    onSuccess: (_, id) => {
      // Invalidate relevant queries
      queryClient.invalidateQueries({ queryKey: ["admin", "vendors"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "vendor-details", id] });
      queryClient.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}

export function useReactivateVendor() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.reactivateVendor(id),
    onSuccess: (_, id) => {
      // Invalidate relevant queries
      queryClient.invalidateQueries({ queryKey: ["admin", "vendors"] });
      queryClient.invalidateQueries({ queryKey: ["admin", "vendor-details", id] });
      queryClient.invalidateQueries({ queryKey: ["admin", "dashboard"] });
    },
  });
}
