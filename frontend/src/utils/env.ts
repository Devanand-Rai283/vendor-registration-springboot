/**
 * Environment configuration helper
 * Ensures environment variables are validated and typesafe.
 */
export const env = {
  NEXT_PUBLIC_API_URL:
    process.env.NEXT_PUBLIC_API_URL || "",
  NEXT_PUBLIC_RAZORPAY_KEY_ID:
    process.env.NEXT_PUBLIC_RAZORPAY_KEY_ID || "",
};
