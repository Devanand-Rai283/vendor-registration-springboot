"use client";

import React, { useState } from "react";
import { useRouter } from "next/navigation";
import { useAdminVendors } from "@/features/admin/hooks/useAdminQueries";
import { VendorTable } from "@/features/admin/components/VendorTable";
import { VendorFilters } from "@/features/admin/components/VendorFilters";
import { SuspendVendorDialog } from "@/features/admin/components/SuspendVendorDialog";
import { ReactivateVendorDialog } from "@/features/admin/components/ReactivateVendorDialog";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { Pagination } from "@/components/ui/pagination";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { VendorStatus, AdminVendorSummary } from "@/features/admin/types";

export default function AdminVendorsPage() {
  const router = useRouter();
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<VendorStatus | "ALL">("ALL");

  // Dialog State
  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false);
  const [reactivateDialogOpen, setReactivateDialogOpen] = useState(false);
  const [selectedVendor, setSelectedVendor] = useState<AdminVendorSummary | null>(null);

  const { data, isLoading, error, refetch } = useAdminVendors(
    page,
    20,
    statusFilter === "ALL" ? undefined : statusFilter
  );

  const handleStatusChange = (status: VendorStatus | "ALL") => {
    setStatusFilter(status);
    setPage(0); // Reset page to first page when filter changes
  };

  const handlePageChange = (newPage: number) => {
    setPage(newPage - 1); // Pagination component is 1-indexed, state is 0-indexed
  };

  const handleViewDetails = (id: string) => {
    router.push(`/admin/vendors/${id}`);
  };

  const handleSuspendClick = (vendor: AdminVendorSummary) => {
    setSelectedVendor(vendor);
    setSuspendDialogOpen(true);
  };

  const handleReactivateClick = (vendor: AdminVendorSummary) => {
    setSelectedVendor(vendor);
    setReactivateDialogOpen(true);
  };

  if (isLoading) {
    return <LoadingState message="Fetching registered vendors..." />;
  }

  if (error) {
    return (
      <ErrorState
        error={error}
        title="Failed to load vendors"
        onRetry={() => refetch()}
      />
    );
  }

  const vendors = data?.content || [];
  const totalPages = data?.totalPages || 0;

  return (
    <div className="space-y-6">
      <div className="border-b border-border pb-6">
        <SectionHeader
          title="Vendor Management"
          subtitle="Monitor vendor statuses, review documentation, and manage login privileges"
        />
      </div>

      {/* Filters */}
      <VendorFilters
        selectedStatus={statusFilter}
        onStatusChange={handleStatusChange}
      />

      {/* Table */}
      <VendorTable
        vendors={vendors}
        onViewDetails={handleViewDetails}
        onSuspend={handleSuspendClick}
        onReactivate={handleReactivateClick}
      />

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center pt-4">
          <Pagination
            currentPage={page + 1}
            totalPages={totalPages}
            onPageChange={handlePageChange}
          />
        </div>
      )}

      {/* Suspend Confirmation Dialog */}
      {selectedVendor && (
        <SuspendVendorDialog
          isOpen={suspendDialogOpen}
          onClose={() => {
            setSuspendDialogOpen(false);
            setSelectedVendor(null);
          }}
          vendorId={selectedVendor.id}
          businessName={selectedVendor.businessName}
        />
      )}

      {/* Reactivate Confirmation Dialog */}
      {selectedVendor && (
        <ReactivateVendorDialog
          isOpen={reactivateDialogOpen}
          onClose={() => {
            setReactivateDialogOpen(false);
            setSelectedVendor(null);
          }}
          vendorId={selectedVendor.id}
          businessName={selectedVendor.businessName}
        />
      )}
    </div>
  );
}
