import React from "react";
import { Users, Store, ClipboardCheck, ShoppingBag } from "lucide-react";
import { AdminStatsCard } from "./AdminStatsCard";
import { AdminDashboardData } from "../types";

interface AdminDashboardMetricsProps {
  data: AdminDashboardData;
}

export function AdminDashboardMetrics({ data }: AdminDashboardMetricsProps) {
  return (
    <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
      <AdminStatsCard
        title="Total Vendors"
        value={data.totalVendors}
        icon={<Store className="h-6 w-6" />}
        description="All registered vendor profiles"
      />
      <AdminStatsCard
        title="Pending Approvals"
        value={data.pendingApprovals}
        icon={<ClipboardCheck className="h-6 w-6" />}
        description="Profiles awaiting admin review"
        className={data.pendingApprovals > 0 ? "border-amber-200 bg-amber-50/5" : ""}
      />
      <AdminStatsCard
        title="Total Users"
        value={data.totalUsers}
        icon={<Users className="h-6 w-6" />}
        description="Customers, vendors, and admins"
      />
      <AdminStatsCard
        title="Orders Today"
        value={data.totalOrdersToday}
        icon={<ShoppingBag className="h-6 w-6" />}
        description="Orders placed during current UTC day"
      />
    </div>
  );
}
