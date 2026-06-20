import React from "react";
import { Badge } from "@/components/ui/badge";
import { VendorStatus, AccountStatus } from "../types";

interface VendorStatusBadgeProps {
  status: VendorStatus;
}

export function VendorStatusBadge({ status }: VendorStatusBadgeProps) {
  const getVariant = (s: VendorStatus) => {
    switch (s) {
      case "APPROVED":
        return "success";
      case "PENDING_REVIEW":
        return "warning";
      case "REJECTED":
        return "danger";
      default:
        return "default";
    }
  };

  const getLabel = (s: VendorStatus) => {
    switch (s) {
      case "APPROVED":
        return "Approved";
      case "PENDING_REVIEW":
        return "Pending Review";
      case "REJECTED":
        return "Rejected";
      default:
        return s;
    }
  };

  return <Badge variant={getVariant(status)}>{getLabel(status)}</Badge>;
}

interface AccountStatusBadgeProps {
  status: AccountStatus;
}

export function AccountStatusBadge({ status }: AccountStatusBadgeProps) {
  const getVariant = (s: AccountStatus) => {
    switch (s) {
      case "ACTIVE":
        return "success";
      case "SUSPENDED":
        return "danger";
      case "INACTIVE":
        return "outline";
      default:
        return "default";
    }
  };

  const getLabel = (s: AccountStatus) => {
    switch (s) {
      case "ACTIVE":
        return "Active";
      case "SUSPENDED":
        return "Suspended";
      case "INACTIVE":
        return "Inactive";
      default:
        return s;
    }
  };

  return <Badge variant={getVariant(status)}>{getLabel(status)}</Badge>;
}
