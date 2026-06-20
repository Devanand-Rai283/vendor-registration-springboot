export type VendorStatus = "PENDING_REVIEW" | "APPROVED" | "REJECTED";
export type AccountStatus = "ACTIVE" | "SUSPENDED" | "INACTIVE";

export interface AdminDashboardData {
  totalVendors: number;
  pendingApprovals: number;
  totalUsers: number;
  totalOrdersToday: number;
}

export interface AdminVendorSummary {
  id: string;
  businessName: string;
  ownerName: string;
  status: VendorStatus;
  userEmail: string | null;
  userAccountStatus: AccountStatus | null;
}

export interface AdminVendorDocument {
  id: string;
  documentType: string;
  status: "PENDING" | "APPROVED" | "REJECTED";
  fileUrl: string;
  uploadedAt: string;
  rejectionReason: string | null;
}

export interface AdminVendorDetail {
  id: string;
  businessName: string;
  ownerName: string;
  email: string;
  phoneNumber: string | null;
  description: string | null;
  foodType: string | null;
  status: VendorStatus;
  accountStatus: AccountStatus;
  address: string | null;
  latitude: number | null;
  longitude: number | null;
  averageRating: number | null;
  totalReviews: number | null;
  createdAt: string;
  updatedAt: string;
  rejectionReason: string | null;
  documents: AdminVendorDocument[];
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    offset: number;
    paged: boolean;
    unpaged: boolean;
  };
  totalPages: number;
  totalElements: number;
  size: number;
  number: number;
  numberOfElements: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}
