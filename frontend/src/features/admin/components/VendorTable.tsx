import React from "react";
import {
  Table,
  TableHeader,
  TableBody,
  TableHead,
  TableRow,
  TableCell,
} from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { VendorStatusBadge, AccountStatusBadge } from "./VendorStatusBadge";
import { AdminVendorSummary } from "../types";
import { Eye, ShieldAlert, ShieldCheck } from "lucide-react";

interface VendorTableProps {
  vendors: AdminVendorSummary[];
  onViewDetails: (id: string) => void;
  onSuspend: (vendor: AdminVendorSummary) => void;
  onReactivate: (vendor: AdminVendorSummary) => void;
}

export function VendorTable({
  vendors,
  onViewDetails,
  onSuspend,
  onReactivate,
}: VendorTableProps) {
  if (vendors.length === 0) {
    return (
      <div className="flex min-h-[300px] flex-col items-center justify-center rounded-xl border border-dashed border-border bg-surface p-8 text-center">
        <p className="text-sm font-semibold text-text-secondary">No vendors matching filter</p>
      </div>
    );
  }

  return (
    <div className="overflow-hidden rounded-xl border border-border bg-surface shadow-xs">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>Business Name</TableHead>
            <TableHead>Owner</TableHead>
            <TableHead>Email</TableHead>
            <TableHead>Vendor Status</TableHead>
            <TableHead>Account Status</TableHead>
            <TableHead className="text-right">Actions</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {vendors.map((vendor) => (
            <TableRow key={vendor.id}>
              <TableCell className="font-semibold text-text-primary">
                {vendor.businessName}
              </TableCell>
              <TableCell className="text-text-secondary">{vendor.ownerName}</TableCell>
              <TableCell className="text-text-secondary">{vendor.userEmail || "—"}</TableCell>
              <TableCell>
                <VendorStatusBadge status={vendor.status} />
              </TableCell>
              <TableCell>
                {vendor.userAccountStatus ? (
                  <AccountStatusBadge status={vendor.userAccountStatus} />
                ) : (
                  <span className="text-text-secondary text-xs">—</span>
                )}
              </TableCell>
              <TableCell className="text-right">
                <div className="flex items-center justify-end gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => onViewDetails(vendor.id)}
                    title="View vendor detail"
                    className="h-8 px-2.5"
                  >
                    <Eye className="mr-1.5 h-3.5 w-3.5" />
                    Details
                  </Button>

                  {vendor.userAccountStatus === "ACTIVE" && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => onSuspend(vendor)}
                      title="Suspend vendor account"
                      className="h-8 px-2.5 border-danger/30 text-danger hover:bg-danger/5 hover:text-danger"
                    >
                      <ShieldAlert className="mr-1.5 h-3.5 w-3.5" />
                      Suspend
                    </Button>
                  )}

                  {vendor.userAccountStatus === "SUSPENDED" && (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => onReactivate(vendor)}
                      title="Reactivate vendor account"
                      className="h-8 px-2.5 border-success/30 text-success hover:bg-success/5 hover:text-success"
                    >
                      <ShieldCheck className="mr-1.5 h-3.5 w-3.5" />
                      Reactivate
                    </Button>
                  )}
                </div>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
