"use client";

import React, { Suspense } from "react";
import { useSearchParams } from "next/navigation";
import PublicLayout from "@/components/layout/PublicLayout";
import AuthCard from "@/components/forms/AuthCard";
import LoginForm from "@/components/forms/LoginForm";
import Link from "next/link";
import { CheckCircle2 } from "lucide-react";

function LoginContent() {
  const searchParams = useSearchParams();
  const registered = searchParams.get("registered") === "true";
  const expired = searchParams.get("expired") === "true";

  return (
    <div className="flex justify-center items-center py-12">
      <AuthCard
        title="Sign In"
        description="Access your Street Vendor account"
      >
        <div className="space-y-4">
          {/* Registration success notice */}
          {registered && (
            <div
              role="alert"
              className="flex items-center gap-3 rounded-lg border border-green-100 bg-green-50/50 p-3.5 text-sm text-green-800 font-semibold"
            >
              <CheckCircle2 className="h-4 w-4 shrink-0 text-green-700" />
              <span>Registration successful! Please log in.</span>
            </div>
          )}
          
          <LoginForm sessionExpired={expired} />
          
          <div className="text-center text-sm text-text-secondary font-medium">
            Don&apos;t have an account?{" "}
            <Link href="/register" className="text-street-blue hover:underline">
              Create one
            </Link>
          </div>
        </div>
      </AuthCard>
    </div>
  );
}

export default function LoginPage() {
  return (
    <PublicLayout>
      <Suspense fallback={<div className="flex justify-center items-center py-20 text-text-secondary font-semibold">Loading...</div>}>
        <LoginContent />
      </Suspense>
    </PublicLayout>
  );
}
