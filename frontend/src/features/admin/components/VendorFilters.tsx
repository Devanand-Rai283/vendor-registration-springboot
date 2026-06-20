import React from "react";
import { Select } from "@/components/ui/select";
import { VendorStatus } from "../types";

interface VendorFiltersProps {
  selectedStatus: VendorStatus | "ALL";
  onStatusChange: (status: VendorStatus | "ALL") => void;
}

export function VendorFilters({
  selectedStatus,
  onStatusChange,
}: VendorFiltersProps) {
  const options = [
    { value: "ALL", label: "All Statuses" },
    { value: "PENDING_REVIEW", label: "Pending Review" },
    { value: "APPROVED", label: "Approved" },
    { value: "REJECTED", label: "Rejected" },
  ];

  return (
    <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 bg-surface p-4 rounded-xl border border-border">
      <div className="space-y-0.5">
        <h3 className="text-sm font-semibold text-text-primary">Filters</h3>
        <p className="text-xs text-text-secondary">Filter vendors by application status</p>
      </div>
      <div className="w-full sm:w-48">
        <Select
          options={options}
          value={selectedStatus}
          onChange={(e) => onStatusChange(e.target.value as VendorStatus | "ALL")}
        />
      </div>
    </div>
  );
}
