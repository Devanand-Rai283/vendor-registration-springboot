import { ApiError } from "@/services/api/types";

/**
 * Format any API Error or Standard Error into a display-friendly message
 */
export function formatErrorMessage(error: unknown): string {
  if (!error) {
    return "An unknown error occurred.";
  }

  // Handle formatted ApiError structure
  if (
    typeof error === "object" &&
    error !== null &&
    "status" in error &&
    "message" in error
  ) {
    const apiError = error as ApiError;
    
    // Check validation error arrays
    if (apiError.errors && Object.keys(apiError.errors).length > 0) {
      const firstKey = Object.keys(apiError.errors)[0];
      const messages = apiError.errors[firstKey];
      if (Array.isArray(messages) && messages.length > 0) {
        return `${firstKey}: ${messages[0]}`;
      }
    }
    
    return apiError.message;
  }

  // Handle standard Javascript Error
  if (error instanceof Error) {
    return error.message;
  }

  // Handle string errors
  if (typeof error === "string") {
    return error;
  }

  return "An unexpected error occurred. Please try again later.";
}
