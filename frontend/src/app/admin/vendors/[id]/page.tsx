"use client";

import React, { useState } from "react";
import { useParams } from "next/navigation";
import { useAdminVendorDetails } from "@/features/admin/hooks/useAdminQueries";
import { VendorStatusBadge, AccountStatusBadge } from "@/features/admin/components/VendorStatusBadge";
import { SuspendVendorDialog } from "@/features/admin/components/SuspendVendorDialog";
import { ReactivateVendorDialog } from "@/features/admin/components/ReactivateVendorDialog";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { Button } from "@/components/ui/button";
import { Table, TableHeader, TableBody, TableHead, TableRow, TableCell } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  ChevronLeft,
  Calendar,
  User,
  Mail,
  Phone,
  Star,
  FileText,
  ExternalLink,
  ShieldAlert,
  ShieldCheck,
  AlertTriangle
} from "lucide-react";
import Link from "next/link";

export default function AdminVendorDetailPage() {
  const { id } = useParams() as { id: string };

  // Dialog State
  const [suspendDialogOpen, setSuspendDialogOpen] = useState(false);
  const [reactivateDialogOpen, setReactivateDialogOpen] = useState(false);

  const { data: vendor, isLoading, error, refetch } = useAdminVendorDetails(id);

  if (isLoading) {
    return <LoadingState message="Loading vendor file and documents..." />;
  }

  if (error || !vendor) {
    return (
      <ErrorState
        error={error || new Error("Vendor details not found")}
        title="Failed to load vendor details"
        onRetry={() => refetch()}
      />
    );
  }

  const getDocStatusVariant = (status: string) => {
    switch (status) {
      case "APPROVED":
        return "success";
      case "PENDING":
        return "warning";
      case "REJECTED":
        return "danger";
      default:
        return "default";
    }
  };

  return (
    <div className="space-y-8">
      {/* Back button */}
      <div>
        <Link
          href="/admin/vendors"
          className="inline-flex items-center gap-1.5 text-sm font-medium text-text-secondary hover:text-text-primary transition-colors"
        >
          <ChevronLeft className="h-4 w-4" /> Back to Vendors List
        </Link>
      </div>

      {/* Header Info Panel */}
      <div className="flex flex-col lg:flex-row justify-between items-start lg:items-center gap-6 bg-surface border border-border p-6 rounded-2xl shadow-xs">
        <div className="min-w-0 flex-1 space-y-3">
          <div className="flex items-center gap-3 flex-wrap">
            <h1 className="text-2xl font-bold text-text-primary">{vendor.businessName}</h1>
            <VendorStatusBadge status={vendor.status} />
            <AccountStatusBadge status={vendor.accountStatus} />
          </div>
          <p className="text-sm text-text-secondary font-medium">
            Food Type: {vendor.foodType || "Unspecified"}
          </p>
          <div className="flex items-center gap-4 text-xs text-text-secondary">
            <span className="inline-flex items-center gap-1">
              <Calendar className="h-3.5 w-3.5" />
              Registered: {new Date(vendor.createdAt).toLocaleDateString()}
            </span>
            <span className="inline-flex items-center gap-1">
              <Star className="h-3.5 w-3.5 fill-amber-500 text-amber-500" />
              {vendor.averageRating ? vendor.averageRating.toFixed(1) : "0.0"} ({vendor.totalReviews || 0} reviews)
            </span>
          </div>
        </div>

        {/* Header Actions */}
        <div className="flex flex-wrap gap-3">
          {vendor.accountStatus === "ACTIVE" ? (
            <Button
              variant="destructive"
              onClick={() => setSuspendDialogOpen(true)}
              className="flex items-center gap-1.5 font-medium shadow-xs"
            >
              <ShieldAlert className="h-4 w-4" />
              Suspend Account
            </Button>
          ) : (
            <Button
              onClick={() => setReactivateDialogOpen(true)}
              className="flex items-center gap-1.5 font-medium shadow-xs bg-street-blue hover:bg-blue-600 text-white"
            >
              <ShieldCheck className="h-4 w-4" />
              Reactivate Account
            </Button>
          )}
        </div>
      </div>

      {/* Rejection Reason Alert if applicable */}
      {vendor.status === "REJECTED" && vendor.rejectionReason && (
        <div className="flex gap-3 border border-red-200 bg-red-50/20 p-4 rounded-xl text-danger">
          <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5" />
          <div>
            <h4 className="font-bold text-sm">Application Rejected</h4>
            <p className="mt-1 text-xs leading-relaxed text-red-700/85">
              Reason: {vendor.rejectionReason}
            </p>
          </div>
        </div>
      )}

      {/* Information Cards Grid */}
      <div className="grid gap-6 md:grid-cols-3">
        {/* Business details */}
        <div className="bg-surface border border-border p-6 rounded-xl space-y-4">
          <h3 className="font-bold text-base border-b border-border/60 pb-2">Business Profile</h3>
          <div className="space-y-3 text-sm">
            <div>
              <span className="block text-xs font-semibold text-text-secondary uppercase">Description</span>
              <p className="mt-1 text-text-primary leading-relaxed">{vendor.description || "No description provided."}</p>
            </div>
            <div>
              <span className="block text-xs font-semibold text-text-secondary uppercase">Address</span>
              <p className="mt-1 text-text-primary leading-relaxed">{vendor.address || "No address provided."}</p>
            </div>
            {vendor.latitude !== null && vendor.longitude !== null && (
              <div>
                <span className="block text-xs font-semibold text-text-secondary uppercase">Coordinates</span>
                <p className="mt-1 text-text-primary">
                  {vendor.latitude.toFixed(6)}, {vendor.longitude.toFixed(6)}
                </p>
              </div>
            )}
          </div>
        </div>

        {/* Owner details */}
        <div className="bg-surface border border-border p-6 rounded-xl space-y-4">
          <h3 className="font-bold text-base border-b border-border/60 pb-2">Owner Profile</h3>
          <div className="space-y-3 text-sm">
            <div className="flex items-center gap-2">
              <User className="h-4 w-4 text-text-secondary" />
              <div>
                <span className="block text-xs text-text-secondary">Owner Name</span>
                <p className="font-medium text-text-primary">{vendor.ownerName}</p>
              </div>
            </div>
          </div>
        </div>

        {/* Contact details */}
        <div className="bg-surface border border-border p-6 rounded-xl space-y-4">
          <h3 className="font-bold text-base border-b border-border/60 pb-2">Contact details</h3>
          <div className="space-y-3 text-sm">
            <div className="flex items-center gap-2">
              <Mail className="h-4 w-4 text-text-secondary" />
              <div>
                <span className="block text-xs text-text-secondary">Email Address</span>
                <p className="font-medium text-text-primary">{vendor.email}</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <Phone className="h-4 w-4 text-text-secondary" />
              <div>
                <span className="block text-xs text-text-secondary">Phone Number</span>
                <p className="font-medium text-text-primary">{vendor.phoneNumber || "—"}</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Verification Documents Section */}
      <div className="space-y-4">
        <div className="flex items-center gap-2 border-b border-border/60 pb-2">
          <FileText className="h-5 w-5 text-text-secondary" />
          <h2 className="text-lg font-bold text-text-primary">Verification Documents</h2>
        </div>

        {!vendor.documents || vendor.documents.length === 0 ? (
          <div className="flex min-h-[200px] flex-col items-center justify-center rounded-xl border border-dashed border-border bg-surface p-8 text-center">
            <p className="text-sm font-semibold text-text-secondary">No documents uploaded</p>
          </div>
        ) : (
          <div className="overflow-hidden rounded-xl border border-border bg-surface shadow-xs">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>Document Type</TableHead>
                  <TableHead>Verification Status</TableHead>
                  <TableHead>Uploaded At</TableHead>
                  <TableHead>Rejection Reason</TableHead>
                  <TableHead className="text-right">Action</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {vendor.documents.map((doc) => (
                  <TableRow key={doc.id}>
                    <TableCell className="font-semibold text-text-primary">
                      {doc.documentType.replace(/_/g, " ")}
                    </TableCell>
                    <TableCell>
                      <Badge variant={getDocStatusVariant(doc.status)}>
                        {doc.status}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-text-secondary">
                      {new Date(doc.uploadedAt).toLocaleString()}
                    </TableCell>
                    <TableCell className="text-xs text-danger max-w-[200px] truncate" title={doc.rejectionReason || undefined}>
                      {doc.rejectionReason || "—"}
                    </TableCell>
                    <TableCell className="text-right">
                      <a
                        href={doc.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="inline-flex h-8 items-center justify-center rounded-lg border border-border bg-surface px-3 text-xs font-medium text-street-blue hover:bg-muted hover:text-blue-700 transition-colors"
                      >
                        <ExternalLink className="mr-1.5 h-3.5 w-3.5" />
                        View File
                      </a>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        )}
      </div>

      {/* Confirmation Dialogs */}
      <SuspendVendorDialog
        isOpen={suspendDialogOpen}
        onClose={() => setSuspendDialogOpen(false)}
        vendorId={vendor.id}
        businessName={vendor.businessName}
      />

      <ReactivateVendorDialog
        isOpen={reactivateDialogOpen}
        onClose={() => setReactivateDialogOpen(false)}
        vendorId={vendor.id}
        businessName={vendor.businessName}
      />
    </div>
  );
}
