import { apiClient } from "@/services/api/apiClient";
import type { ApiResponse, PaginatedResponse } from "@/services/api/types";
import type {
  NearbyVendorResponse,
  Vendor,
  VendorMenuResponse,
  FoodSearchItem,
  VendorReviewResponse,
} from "@/features/vendors/types";

/**
 * GET /api/vendors/nearby
 * Backend: Returns ApiResponse<NearbyVendorResponse>
 * Response body: { success, message, data: { vendors: [...], page, size, totalElements, totalPages }, timestamp }
 */
export async function getNearbyVendors(params?: {
  latitude?: number;
  longitude?: number;
  radius?: number;
  page?: number;
  size?: number;
}): Promise<NearbyVendorResponse> {
  const searchParams = new URLSearchParams();
  if (params?.latitude !== undefined) searchParams.set("lat", String(params.latitude));
  if (params?.longitude !== undefined) searchParams.set("lng", String(params.longitude));
  if (params?.radius !== undefined) searchParams.set("radius", String(params.radius));
  if (params?.page !== undefined) searchParams.set("page", String(params.page));
  if (params?.size !== undefined) searchParams.set("size", String(params.size));

  const query = searchParams.toString();
  const response = await apiClient.get<NearbyVendorResponse>(
    `vendors/nearby${query ? `?${query}` : ""}`
  );
  return response;
}

/**
 * GET /api/vendors/{id}
 * Backend: Returns ApiResponse<Vendor> (vendor detail)
 */
export async function getVendorDetails(id: string): Promise<Vendor> {
  const response = await apiClient.get<ApiResponse<Vendor>>(`vendors/${id}`);
  return response.data;
}

/**
 * GET /api/vendors/{id}/menu
 * Backend: Returns ApiResponse<VendorMenuResponse>
 * VendorMenuResponse: { vendorId, vendorName, categories: [{ id, name, displayOrder, items: [...] }] }
 */
export async function getVendorMenu(
  id: string,
  params?: { page?: number; size?: number }
): Promise<VendorMenuResponse> {
  const searchParams = new URLSearchParams();
  if (params?.page) searchParams.set("page", String(params.page));
  if (params?.size) searchParams.set("size", String(params.size));

  const query = searchParams.toString();
  const response = await apiClient.get<VendorMenuResponse>(
    `vendors/${id}/menu${query ? `?${query}` : ""}`
  );
  return response;
}

/**
 * GET /api/search?q=...
 * Backend: Returns Spring Page<FoodSearchResponseDto>
 * JSON shape: { content: FoodSearchItem[], totalElements, totalPages, number, size, ... }
 * (NOT wrapped in ApiResponse — Spring Page serialized directly by the controller)
 */
export async function searchFood(params: {
  q: string;
  foodType?: string;
  dietaryTag?: string;
  page?: number;
  size?: number;
}): Promise<PaginatedResponse<FoodSearchItem>> {
  const searchParams = new URLSearchParams();
  searchParams.set("keyword", params.q);
  if (params.foodType && params.foodType !== "All") {
    searchParams.set("foodType", params.foodType.toUpperCase() === "VEGETARIAN" ? "VEGETARIAN" : params.foodType);
  }
  if (params.dietaryTag && params.dietaryTag !== "All") {
    searchParams.set("dietaryTag", params.dietaryTag);
  }
  if (params.page !== undefined) searchParams.set("page", String(params.page));
  if (params.size !== undefined) searchParams.set("size", String(params.size));

  return apiClient.get<PaginatedResponse<FoodSearchItem>>(
    `search?${searchParams.toString()}`
  );
}

/**
 * GET /api/vendors/{vendorId}/ratings
 * Backend: Returns Spring Page<VendorReviewResponse>
 */
export async function getVendorReviews(
  vendorId: string,
  page?: number,
  size?: number
): Promise<PaginatedResponse<VendorReviewResponse>> {
  const searchParams = new URLSearchParams();
  if (page !== undefined) searchParams.set("page", String(page));
  if (size !== undefined) searchParams.set("size", String(size));

  const query = searchParams.toString();
  return apiClient.get<PaginatedResponse<VendorReviewResponse>>(
    `vendors/${vendorId}/ratings${query ? `?${query}` : ""}`
  );
}
