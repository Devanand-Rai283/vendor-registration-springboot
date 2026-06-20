"use client";

import React, { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuthStore } from "@/store/authStore";
import { Loader2 } from "lucide-react";

interface AuthGuardProps {
  children: React.ReactNode;
}

export function AuthGuard({ children }: AuthGuardProps) {
  const router = useRouter();
  const { isAuthenticated, accessToken } = useAuthStore();

  useEffect(() => {
    // Redirection check if access token or authorization fails
    if (!isAuthenticated || !accessToken) {
      router.push("/login");
    }
  }, [isAuthenticated, accessToken, router]);

  // Loading indicator panel
  if (!isAuthenticated || !accessToken) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-background">
        <div className="flex flex-col items-center gap-4 text-center">
          <Loader2 className="h-8 w-8 animate-spin text-street-blue" />
          <p className="text-sm font-semibold text-text-secondary tracking-wide">
            Verifying secure session...
          </p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

export default AuthGuard;
