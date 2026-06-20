/* ── Backend DTO alignment ── */

/* GET /api/vendors/nearby → ApiResponse<NearbyVendorResponse> */
export interface NearbyVendorResponse {
  vendors: VendorSummary[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/* Individual vendor inside NearbyVendorResponse.vendors */
export interface VendorSummary {
  id: string;
  businessName: string;
  foodType: string;
  address: string;
  averageRating: number;
  latitude: number;
  longitude: number;
  distanceKm: number;
}

/*
 * Extended vendor detail — combines fields from entity + discovery DTOs
 * Backend DTO sources:
 *  - Vendor entity:      id, businessName, description, phone, foodType,
 *                        averageRating, latitude, longitude, address, status, createdAt
 *  - VendorSummaryResponse: id, businessName, foodType, address, averageRating,
 *                           latitude, longitude, distanceKm
 *  - VendorResponse (admin): vendorId, status, message, rejectionReason
 */
export interface Vendor {
  id: string;
  businessName: string;
  description?: string;
  phone?: string;
  foodType?: string;
  averageRating?: number;
  distance?: number;
  imageUrl?: string;
  coverImageUrl?: string;
  status?: "PENDING_REVIEW" | "APPROVED" | "REJECTED";
  latitude?: number;
  longitude?: number;
  address?: string;
  operatingHours?: string;
  categories?: string[];
  createdAt?: string;
}

/*
 * Menu item — discovery variant (MenuItemResponseDto)
 * Backend DTO source: com.streetvendor.discovery.dto.MenuItemResponseDto
 * Fields: id, name, description, price, dietaryTag, imageUrl, available
 */
export interface MenuItem {
  id: string;
  name: string;
  description: string;
  price: number;
  dietaryTag?: string;
  imageUrl?: string;
  available: boolean;
}

/* Menu category wrapping items */
export interface MenuCategory {
  id: string;
  name: string;
  displayOrder: number;
  items: MenuItem[];
}

/* GET /api/vendors/{id}/menu → ApiResponse<VendorMenuResponse> */
export interface VendorMenuResponse {
  vendorId: string;
  vendorName: string;
  categories: MenuCategory[];
}

/*
 * Food search item — corresponds to FoodSearchResponseDto
 * Backend DTO source: com.streetvendor.discovery.dto.FoodSearchResponseDto
 * Fields: menuItemId, itemName, description, price, dietaryTag,
 *         vendorId, vendorName, foodType, averageRating
 *
 * GET /api/search?q=... returns Spring Page<FoodSearchResponseDto>
 * The JSON shape is: { content: FoodSearchItem[], totalElements, totalPages, number, size, ... }
 */
export interface FoodSearchItem {
  menuItemId: string;
  itemName: string;
  description: string;
  price: number;
  dietaryTag?: string;
  vendorId: string;
  vendorName: string;
  foodType: string;
  averageRating: number;
}

/* GET /api/vendors/{vendorId}/ratings → PaginatedResponse<VendorReviewResponse> */
export interface VendorReviewResponse {
  id: string;
  stars: number;
  reviewText: string;
  customerDisplayName: string;
  createdAt: string;
}
