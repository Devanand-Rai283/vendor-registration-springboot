export const APP_NAME = "Street Vendor Platform";

export const ROLES = {
  CUSTOMER: "CUSTOMER",
  VENDOR: "VENDOR",
  ADMIN: "ADMIN",
} as const;

export const STORAGE_KEYS = {
  TOKEN: "svp_auth_token",
  USER: "svp_user_data",
  THEME: "svp_theme",
} as const;

export const DEFAULT_PAGE_SIZE = 10;
export const API_TIMEOUT_MS = 15000;
