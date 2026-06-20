import { apiClient } from "@/services/api/apiClient";
import { ApiResponse } from "@/services/api/types";
import { useAuthStore, UserRole, User } from "@/store/authStore";

export interface LoginResponseData {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

export interface RegisterResponseData {
  id: string;
  email: string;
  role: UserRole;
  name?: string;
}

/**
 * Decodes a JWT token payload locally to extract user profile and role claims.
 */
export function decodeJwt(token: string): { sub?: string; email?: string; role?: UserRole } | null {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const jsonPayload = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + ("00" + c.charCodeAt(0).toString(16)).slice(-2))
        .join("")
    );
    return JSON.parse(jsonPayload);
  } catch (error) {
    console.error("Failed to decode JWT:", error);
    return null;
  }
}

export const authService = {
  /**
   * Performs credential login call and populates the authStore with parsed user session claims.
   */
  login: async (email: string, password: string): Promise<ApiResponse<LoginResponseData>> => {
    const response = await apiClient.post<ApiResponse<LoginResponseData>>("api/auth/login", {
      email,
      password,
    });

    const token = response.data.accessToken;
    const decoded = decodeJwt(token);

    if (!decoded || !decoded.role) {
      throw new Error("Invalid JWT token received from authentication server.");
    }

    const user: User = {
      id: decoded.sub || "",
      email: decoded.email || email,
      name: decoded.email ? decoded.email.split("@")[0] : "User",
      role: decoded.role,
    };

    // Store user session in memory
    useAuthStore.getState().setAuth(user, token);

    return response;
  },

  /**
   * Registers a new customer or vendor user profile.
   */
  register: async (email: string, password: string, role: "CUSTOMER" | "VENDOR"): Promise<ApiResponse<RegisterResponseData>> => {
    return apiClient.post<ApiResponse<RegisterResponseData>>("api/auth/register", {
      email,
      password,
      role,
    });
  },

  /**
   * Calls the backend logout endpoint to clear HttpOnly cookies and blacklists refresh token,
   * then resets the frontend Zustand auth state in-memory.
   */
  logout: async (): Promise<void> => {
    try {
      await apiClient.post<ApiResponse<void>>("api/auth/logout");
    } catch (error) {
      console.warn("Backend logout request encountered an issue, performing local state cleanup.", error);
    } finally {
      useAuthStore.getState().clearAuth();
    }
  },

  /**
   * Silent refresh request using the backend HttpOnly refresh cookie to request a new access token.
   */
  refresh: async (): Promise<ApiResponse<LoginResponseData>> => {
    const response = await apiClient.post<ApiResponse<LoginResponseData>>("api/auth/refresh");
    const token = response.data.accessToken;
    
    // Update the Zustand in-memory access token
    useAuthStore.getState().setAccessToken(token);
    
    return response;
  },
};
