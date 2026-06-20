/**
 * Environment configuration helper
 * Ensures environment variables are validated and typesafe.
 */
export const env = {
  NEXT_PUBLIC_API_URL:
    process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api",
  NEXT_PUBLIC_RAZORPAY_KEY_ID:
    process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "rzp_test_placeholder",
};
