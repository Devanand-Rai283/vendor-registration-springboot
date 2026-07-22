"use client";

import React, { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useRouter } from "next/navigation";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { authService } from "@/services/auth/authService";
import { useAuthStore } from "@/store/authStore";
import { Loader2, AlertCircle } from "lucide-react";

// Form validation schema via Zod
export const loginSchema = z.object({
  email: z
    .string()
    .min(1, "Email is required")
    .email("Invalid email address"),
  password: z
    .string()
    .min(6, "Password must be at least 6 characters"),
});

export type LoginFields = z.infer<typeof loginSchema>;

interface LoginFormProps {
  sessionExpired?: boolean;
}

export function LoginForm({ sessionExpired = false }: LoginFormProps) {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [apiError, setApiError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginFields>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: "",
      password: "",
    },
  });

  const onSubmit = async (data: LoginFields) => {
    setLoading(true);
    setApiError(null);
    try {
      await authService.login(data.email, data.password);
      const role = useAuthStore.getState().role;

      // Role Routing: ADMIN -> /admin, VENDOR -> /vendor/onboarding, CUSTOMER -> /
      if (role === "ADMIN") {
        router.push("/admin");
      } else if (role === "VENDOR") {
        router.push("/vendor/onboarding");
      } else {
        router.push("/");
      }
    } catch (error: unknown) {
      console.error("Login failure:", error);
      const err = error as { status?: number; message?: string };
      if (err && err.status === 401) {
        setApiError("Invalid email or password");
      } else {
        setApiError(err?.message || "An unexpected error occurred. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)} className="space-y-4 text-left" noValidate>
      {/* Session Expired / Global Error Banner */}
      {sessionExpired && !apiError && (
        <div
          role="alert"
          className="flex items-center gap-3 rounded-lg border border-amber-100 bg-amber-50/50 p-3.5 text-sm text-amber-800 font-semibold"
        >
          <AlertCircle className="h-4 w-4 shrink-0 text-amber-700" />
          <span>Your session has expired. Please log in again.</span>
        </div>
      )}

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

      {/* Password Field */}
      <div className="space-y-1.5">
        <div className="flex items-center justify-between">
          <label htmlFor="password" className="text-sm font-bold text-text-primary">
            Password
          </label>
        </div>
        <Input
          id="password"
          type="password"
          autoComplete="current-password"
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

      {/* Submit Action */}
      <Button
        type="submit"
        disabled={loading}
        className="w-full rounded-xl py-5 text-sm font-bold shadow-md bg-street-blue hover:bg-blue-700 text-white transition-all focus:ring-2 focus:ring-offset-2 focus:ring-street-blue"
      >
        {loading ? (
          <>
            <Loader2 className="mr-2 h-4 w-4 animate-spin" /> Signing In...
          </>
        ) : (
          "Sign In"
        )}
      </Button>
    </form>
  );
}

export default LoginForm;
