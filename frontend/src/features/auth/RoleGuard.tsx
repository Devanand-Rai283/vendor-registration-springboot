"use client";

import React from "react";
import { useAuthStore, UserRole } from "@/store/authStore";
import { ShieldAlert } from "lucide-react";

interface RoleGuardProps {
  children: React.ReactNode;
  allowedRoles: UserRole[];
}

export function RoleGuard({ children, allowedRoles }: RoleGuardProps) {
  const { user, isAuthenticated } = useAuthStore();

  if (!isAuthenticated || !user) {
    return null;
  }

  const hasAccess = allowedRoles.includes(user.role);

  if (!hasAccess) {
    return (
      <div className="flex min-h-[450px] flex-col items-center justify-center rounded-2xl border border-border bg-surface p-8 text-center shadow-xs">
        <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger/10 text-danger mb-4">
          <ShieldAlert className="h-6 w-6" />
        </div>
        <h2 className="mt-4 text-xl font-bold text-text-primary">Access Denied</h2>
        <p className="mt-2 text-sm text-text-secondary max-w-sm font-medium">
          You do not have the permissions required to access this resource. Please contact your admin if you believe this is an error.
        </p>
      </div>
    );
  }

  return <>{children}</>;
}

export default RoleGuard;
