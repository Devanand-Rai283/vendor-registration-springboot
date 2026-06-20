import { apiClient } from "@/services/api/apiClient";

export interface SubmitReviewRequest {
  orderId: string;
  stars: number;
  reviewText?: string;
}

export interface RatingResponse {
  id: string;
  orderId: string;
  customerId: string;
  vendorId: string;
  stars: number;
  reviewText?: string;
  createdAt: string;
}

export const ratingApi = {
  submitReview: async (request: SubmitReviewRequest): Promise<RatingResponse> => {
    // The backend POST /api/ratings endpoint
    const response = await apiClient.post<RatingResponse>('/ratings', request);
    return response;
  },
};
