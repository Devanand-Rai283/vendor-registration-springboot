import { useQuery } from "@tanstack/react-query";
import {
  getNearbyVendors,
  getVendorDetails,
  getVendorMenu,
  searchFood,
} from "@/features/vendors/api/vendorApi";

const DISCOVERY_QUERY_CONFIG = {
  staleTime: 5 * 60 * 1000, // 5 minutes
  retry: 1,
  refetchOnWindowFocus: false,
};

export function useNearbyVendors(params?: {
  latitude?: number;
  longitude?: number;
  radius?: number;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ["vendors", "nearby", params],
    queryFn: () => getNearbyVendors(params),
    ...DISCOVERY_QUERY_CONFIG,
  });
}

export function useVendorDetails(id: string) {
  return useQuery({
    queryKey: ["vendors", id],
    queryFn: () => getVendorDetails(id),
    enabled: !!id,
    ...DISCOVERY_QUERY_CONFIG,
  });
}

export function useVendorMenu(
  id: string,
  params?: { page?: number; size?: number }
) {
  return useQuery({
    queryKey: ["vendors", id, "menu", params],
    queryFn: () => getVendorMenu(id, params),
    enabled: !!id,
    ...DISCOVERY_QUERY_CONFIG,
  });
}

export function useFoodSearch(params: {
  q: string;
  foodType?: string;
  dietaryTag?: string;
  page?: number;
  size?: number;
}) {
  return useQuery({
    queryKey: ["search", params],
    queryFn: () => searchFood(params),
    enabled: !!params.q && params.q.length > 0,
    ...DISCOVERY_QUERY_CONFIG,
  });
}
