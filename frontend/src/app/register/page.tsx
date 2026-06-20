"use client";

import React from "react";
import PublicLayout from "@/components/layout/PublicLayout";
import AuthCard from "@/components/forms/AuthCard";
import RegisterForm from "@/components/forms/RegisterForm";
import Link from "next/link";

export default function RegisterPage() {
  return (
    <PublicLayout>
      <div className="flex justify-center items-center py-12">
        <AuthCard
          title="Create Account"
          description="Register as a Customer or Vendor partner"
        >
          <div className="space-y-4">
            <RegisterForm />
            
            <div className="text-center text-sm text-text-secondary font-medium">
              Already have an account?{" "}
              <Link href="/login" className="text-street-blue hover:underline">
                Sign In
              </Link>
            </div>
          </div>
        </AuthCard>
      </div>
    </PublicLayout>
  );
}
