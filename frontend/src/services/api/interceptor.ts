import { useAuthStore } from "@/store/authStore";
import { ApiError } from "./types";

export interface RequestOptions extends RequestInit {
  headers?: Record<string, string>;
}

/**
 * Request interceptor to inject the JWT Authorization header and default headers
 */
export function applyRequestInterceptors(options: RequestOptions): RequestOptions {
  const accessToken = useAuthStore.getState().accessToken;
  
  const headers: Record<string, string> = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  if (accessToken) {
    headers["Authorization"] = `Bearer ${accessToken}`;
  }

  return {
    ...options,
    headers,
  };
}

/**
 * Response interceptor to format errors and handle common status codes (401, 403, 500)
 */
export async function handleResponseInterceptor(response: Response): Promise<Response> {
  if (response.ok) {
    return response;
  }

  let errorData: Partial<ApiError> = {};
  try {
    errorData = await response.json();
  } catch {
    // Parse error failed or endpoint returned empty/non-JSON error
  }

  const apiError: ApiError = {
    status: response.status,
    message: errorData.message || response.statusText || "An unexpected error occurred",
    errors: errorData.errors,
    timestamp: errorData.timestamp || new Date().toISOString(),
    path: errorData.path || response.url,
  };

  // Route protection / token refresh hook placeholders
  // 401 errors are thrown as ApiError and recovered in apiClient's refresh queue handler.
  
  throw apiError;
}
