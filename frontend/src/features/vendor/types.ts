export interface VendorDashboardMetrics {
  activeOrders: number;
  totalOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
  averageRating: number;
  totalReviews: number;
}

export interface VendorProfile {
  id: string;
  businessName: string;
  ownerName: string;
  phone: string;
  foodType: string;
  description: string;
  address: string;
  latitude: number;
  longitude: number;
  status: "PENDING_REVIEW" | "APPROVED" | "REJECTED";
  averageRating: number;
  totalReviews: number;
  rejectionReason?: string;
}

export interface UpdateVendorProfileRequest {
  businessName: string;
  ownerName: string;
  phone: string;
  foodType: string;
  description: string;
  address: string;
  latitude: number;
  longitude: number;
}

export interface VendorOrderSummary {
  orderId: string;
  customerId: string;
  customerName: string;
  status: "PENDING" | "ACCEPTED" | "PREPARING" | "READY" | "COMPLETED" | "CANCELLED";
  paymentStatus: "PENDING" | "COMPLETED" | "FAILED" | "REFUNDED";
  totalAmount: number;
  createdAt: string;
}

export interface VendorOrderItem {
  menuItemId: string;
  itemName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface VendorOrderDetail {
  orderId: string;
  customerId: string;
  customerName: string;
  customerPhone: string;
  status: VendorOrderSummary["status"];
  paymentStatus: VendorOrderSummary["paymentStatus"];
  totalAmount: number;
  notes?: string;
  items: VendorOrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface VendorDocument {
  documentId: string;
  documentType: "ID_PROOF" | "BUSINESS_LICENSE" | "FOOD_SAFETY_CERTIFICATE";
  verificationStatus: "PENDING" | "VERIFIED" | "REJECTED";
  uploadedAt: string;
  viewUrl: string | null;
}

export interface AnalyticsSnapshot {
  date: string;
  totalOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
  topMenuItemId?: string;
  topMenuItemName?: string;
  peakHour?: number;
}

export interface AnalyticsResponse {
  vendorId: string;
  snapshots: AnalyticsSnapshot[];
  periodDays: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
