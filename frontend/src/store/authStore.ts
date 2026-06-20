import { create } from "zustand";

export type UserRole = "CUSTOMER" | "VENDOR" | "ADMIN";

export interface User {
  id: string;
  email: string;
  name: string;
  role: UserRole;
}

interface AuthState {
  user: User | null;
  role: UserRole | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  
  // Set authentication details on successful login
  setAuth: (user: User, accessToken: string) => void;
  // Clear authentication details on logout / session expiry
  clearAuth: () => void;
  // Update token during refresh without changing user session details
  setAccessToken: (accessToken: string) => void;
}

// In-Memory Zustand Auth Store: No persistence middleware (localStorage/sessionStorage) is applied.
export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  role: null,
  accessToken: null,
  isAuthenticated: false,

  setAuth: (user, accessToken) =>
    set({
      user,
      role: user.role,
      accessToken,
      isAuthenticated: true,
    }),

  clearAuth: () =>
    set({
      user: null,
      role: null,
      accessToken: null,
      isAuthenticated: false,
    }),

  setAccessToken: (accessToken) =>
    set({
      accessToken,
    }),
}));
