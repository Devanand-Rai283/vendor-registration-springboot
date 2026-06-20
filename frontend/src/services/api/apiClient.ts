import { applyRequestInterceptors, handleResponseInterceptor, RequestOptions } from "./interceptor";
import { env } from "@/utils/env";

let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (error: unknown) => void;
}> = [];

/**
 * Process the queue of pending requests when a token refresh completes.
 */
const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((promise) => {
    if (error) {
      promise.reject(error);
    } else {
      promise.resolve(token!);
    }
  });
  failedQueue = [];
};

/**
 * Standard fetch request wrapper that coordinates interceptors, error handling,
 * and automatic 401 token refresh queue replay.
 */
async function request<T>(url: string, options: RequestOptions = {}): Promise<T> {
  const finalOptions = applyRequestInterceptors(options);
  const baseURL = env.NEXT_PUBLIC_API_URL;
  
  // Format target URL
  const targetURL = url.startsWith("http") ? url : `${baseURL.replace(/\/$/, "")}/${url.replace(/^\//, "")}`;

  try {
    const response = await fetch(targetURL, finalOptions);
    const interceptedResponse = await handleResponseInterceptor(response);
    
    // Parse response JSON data
    const text = await interceptedResponse.text();
    return text ? (JSON.parse(text) as T) : ({} as T);
  } catch (error: unknown) {
    const err = error as { status?: number; message?: string };
    
    // If request fails with 401 and is not login/refresh, attempt silent refresh
    const isAuthRequest = url.includes("api/auth/login") || url.includes("api/auth/refresh");
    
    if (err && err.status === 401 && !isAuthRequest) {
      if (isRefreshing) {
        // Queue this request and replay it once the token refresh succeeds
        return new Promise<string>((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((newToken) => {
          const retriedOptions = {
            ...options,
            headers: {
              ...options.headers,
              Authorization: `Bearer ${newToken}`,
            },
          };
          return request<T>(url, retriedOptions);
        });
      }

      isRefreshing = true;

      try {
        // Dynamically import authService to resolve circular dependencies
        const { authService } = await import("@/services/auth/authService");
        const refreshResponse = await authService.refresh();
        const newToken = refreshResponse.data.accessToken;

        // Explicitly update the active token in the store before replaying the queue
        const { useAuthStore } = await import("@/store/authStore");
        useAuthStore.getState().setAccessToken(newToken);

        isRefreshing = false;
        processQueue(null, newToken);

        // Replay the original request with the new access token
        const retriedOptions = {
          ...options,
          headers: {
            ...options.headers,
            Authorization: `Bearer ${newToken}`,
          },
        };
        return request<T>(url, retriedOptions);
      } catch (refreshError: unknown) {
        isRefreshing = false;
        processQueue(refreshError, null);

        // Logout locally and redirect with query param for zero-persistence message delivery
        const { useAuthStore } = await import("@/store/authStore");
        useAuthStore.getState().clearAuth();

        if (typeof window !== "undefined") {
          window.location.href = "/login?expired=true";
        }

        throw refreshError;
      }
    }

    throw error;
  }
}

export const apiClient = {
  get: <T>(url: string, options?: RequestOptions) => 
    request<T>(url, { ...options, method: "GET" }),
  
  post: <T>(url: string, data?: unknown, options?: RequestOptions) => 
    request<T>(url, { 
      ...options, 
      method: "POST", 
      body: data ? JSON.stringify(data) : undefined 
    }),
  
  put: <T>(url: string, data?: unknown, options?: RequestOptions) => 
    request<T>(url, { 
      ...options, 
      method: "PUT", 
      body: data ? JSON.stringify(data) : undefined 
    }),
  
  patch: <T>(url: string, data?: unknown, options?: RequestOptions) => 
    request<T>(url, { 
      ...options, 
      method: "PATCH", 
      body: data ? JSON.stringify(data) : undefined 
    }),
  
  delete: <T>(url: string, options?: RequestOptions) => 
    request<T>(url, { ...options, method: "DELETE" }),
};
