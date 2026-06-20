"use client";

import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { authService } from "@/services/auth/authService";
import { Loader2, AlertCircle } from "lucide-react";

// Form validation schema via Zod
export const registerSchema = z
  .object({
    email: z
      .string()
      .min(1, "Email is required")
      .email("Invalid email address"),
    password: z
      .string()
      .min(6, "Password must be at least 6 characters"),
    confirmPassword: z
      .string()
      .min(1, "Please confirm your password"),
    role: z.enum(["CUSTOMER", "VENDOR"]),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: "Passwords do not match",
    path: ["confirmPassword"],
  });

export type RegisterFields = z.infer<typeof registerSchema>;

export function RegisterForm() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFields>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: "",
      password: "",
      confirmPassword: "",
      role: "CUSTOMER",
    },
  });

  const onSubmit = async (data: RegisterFields) => {
    setLoading(true);
    setApiError(null);
    try {
      await authService.register(data.email, data.password, data.role);
      // Success redirection to /login. No automatic login.
      router.push("/login?registered=true");
    } catch (error: unknown) {
      console.error("Registration failure:", error);
      const err = error as { message?: string };
      // Display backend validation message
      setApiError(err?.message || "An unexpected error occurred. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 text-left" noValidate>
      {apiError && (
        <div
          role="alert"
          className="flex items-center gap-3 rounded-lg border border-red-100 bg-red-50/50 p-3.5 text-sm text-red-800 font-semibold"
        >
          <AlertCircle className="h-4 w-4 shrink-0 text-red-700" />
          <span>{apiError}</span>
        </div>
      )}

      {/* Email Field */}
      <div className="space-y-1.5">
        <label htmlFor="email" className="text-sm font-bold text-text-primary">
          Email Address
        </label>
        <Input
          id="email"
          type="email"
          autoComplete="email"
          placeholder="name@example.com"
          aria-invalid={errors.email ? "true" : "false"}
          aria-describedby={errors.email ? "email-error" : undefined}
          className="rounded-lg focus:ring-street-blue"
          {...register("email")}
        />
        {errors.email && (
          <p id="email-error" className="text-xs font-semibold text-danger">
            {errors.email.message}
          </p>
        )}
      </div>

      {/* Role Selection */}
      <div className="space-y-1.5">
        <label htmlFor="role" className="text-sm font-bold text-text-primary">
          Account Type
        </label>
        <div className="relative">
          <select
            id="role"
            className="flex h-9 w-full rounded-lg border border-border bg-surface px-3 py-1 text-sm shadow-xs transition-colors focus-visible:outline-hidden focus-visible:ring-1 focus-visible:ring-ring text-text-primary font-medium"
            {...register("role")}
          >
            <option value="CUSTOMER">Customer Profile</option>
            <option value="VENDOR">Vendor Partner</option>
          </select>
        </div>
        {errors.role && (
          <p id="role-error" className="text-xs font-semibold text-danger">
            {errors.role.message}
          </p>
        )}
      </div>

      {/* Password Field */}
      <div className="space-y-1.5">
        <label htmlFor="password" className="text-sm font-bold text-text-primary">
          Password
        </label>
        <Input
          id="password"
          type="password"
          placeholder="••••••••"
          aria-invalid={errors.password ? "true" : "false"}
          aria-describedby={errors.password ? "password-error" : undefined}
          className="rounded-lg focus:ring-street-blue"
          {...register("password")}
        />
        {errors.password && (
          <p id="password-error" className="text-xs font-semibold text-danger">
            {errors.password.message}
          </p>
        )}
      </div>

      {/* Confirm Password Field */}
      <div className="space-y-1.5">
        <label htmlFor="confirmPassword" className="text-sm font-bold text-text-primary">
          Confirm Password
        </label>
        <Input
          id="confirmPassword"
          type="password"
          placeholder="••••••••"
          aria-invalid={errors.confirmPassword ? "true" : "false"}
          aria-describedby={errors.confirmPassword ? "confirmPassword-error" : undefined}
          className="rounded-lg focus:ring-street-blue"
          {...register("confirmPassword")}
        />
        {errors.confirmPassword && (
          <p id="confirmPassword-error" className="text-xs font-semibold text-danger">
            {errors.confirmPassword.message}
          </p>
        )}
      </div>

      {/* Submit Action */}
      <Button
        type="submit"
        disabled={loading}
        className="w-full rounded-xl py-5 text-sm font-bold shadow-md bg-street-blue hover:bg-blue-700 text-white transition-all focus:ring-2 focus:ring-offset-2 focus:ring-street-blue"
      >
        {loading ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Registering...
          </>
        ) : (
          "Register Account"
        )}
      </Button>
    </form>
  );
}

export default RegisterForm;
