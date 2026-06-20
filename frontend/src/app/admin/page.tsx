"use client";

import React from "react";
import Link from "next/link";
import { useAdminDashboard } from "@/features/admin/hooks/useAdminQueries";
import { AdminDashboardMetrics } from "@/features/admin/components/AdminDashboardMetrics";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { Button } from "@/components/ui/button";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { Store, ChevronRight } from "lucide-react";

export default function AdminDashboardPage() {
  const { data, isLoading, error, refetch } = useAdminDashboard();

  if (isLoading) {
    return <LoadingState message="Loading dashboard overview metrics..." />;
  }

  if (error) {
    return (
      <ErrorState
        error={error}
        title="Dashboard Failure"
        onRetry={() => refetch()}
      />
    );
  }

  return (
    <div className="space-y-8">
      {/* Banner / Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-border pb-6">
        <SectionHeader
          title="Admin Overview"
          subtitle="Platform-wide statistics and management actions"
        />
        <div className="flex items-center gap-3">
          <Link href="/admin/vendors">
            <Button className="bg-street-blue hover:bg-blue-600 text-white font-medium shadow-xs">
              <Store className="mr-2 h-4 w-4" />
              Manage Vendors
              <ChevronRight className="ml-1.5 h-4 w-4" />
            </Button>
          </Link>
        </div>
      </div>

      {/* Metrics overview */}
      {data && <AdminDashboardMetrics data={data} />}

      {/* Quick Info Grid */}
      <div className="grid gap-6 md:grid-cols-2">
        <div className="bg-surface rounded-xl border border-border p-6 shadow-2xs">
          <h3 className="font-bold text-lg text-text-primary mb-2">Vendor Approvals</h3>
          <p className="text-sm text-text-secondary mb-4 leading-relaxed">
            Review registration applications submitted by new micro-entrepreneurs. Check certificates, contact details, and locations to ensure quality and compliance.
          </p>
          <Link href="/admin/vendors?status=PENDING_REVIEW">
            <Button variant="outline" size="sm">
              View Pending Approvals ({data?.pendingApprovals || 0})
            </Button>
          </Link>
        </div>

        <div className="bg-surface rounded-xl border border-border p-6 shadow-2xs">
          <h3 className="font-bold text-lg text-text-primary mb-2">Platform Policy</h3>
          <p className="text-sm text-text-secondary mb-4 leading-relaxed">
            Suspension of a vendor&apos;s account blocks active login access and invalidates active authentication tokens. Reactivate accounts to restore privileges.
          </p>
          <Link href="/admin/vendors">
            <Button variant="outline" size="sm">
              Manage Accounts
            </Button>
          </Link>
        </div>
      </div>
    </div>
  );
}
