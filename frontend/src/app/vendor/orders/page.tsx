"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useVendorOrders } from "@/features/vendor/hooks/useVendorPortalQueries";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { StatusChip } from "@/components/ui/status-chip";
import { Pagination } from "@/components/ui/pagination";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { Search, Eye, ShoppingBag } from "lucide-react";

export default function VendorOrdersPage() {
  const [page, setPage] = useState(0);
  const [statusFilter, setStatusFilter] = useState<string>("");
  const [searchTerm, setSearchTerm] = useState("");

  const { data, isLoading, isError, error, refetch } = useVendorOrders(page, 20, statusFilter || undefined);

  const getOrderStatusVariant = (status: string): "SUCCESS" | "WARNING" | "DANGER" | "INFO" => {
    switch (status) {
      case 'COMPLETED': return 'SUCCESS';
      case 'CANCELLED': return 'DANGER';
      case 'PENDING': return 'WARNING';
      default: return 'INFO';
    }
  };

  if (isLoading) {
    return <LoadingState message="Loading orders..." />;
  }

  if (isError || !data) {
    return (
      <ErrorState
        title="Failed to load orders"
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  // Client-side filtering by customer name
  const filteredOrders = data.content.filter((order) =>
    order.customerName.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-text-primary">Orders Console</h1>
          <p className="text-text-secondary mt-1">Manage and track your incoming orders.</p>
        </div>
      </div>

      <Card>
        <CardHeader className="border-b border-border pb-4">
          <div className="flex flex-col sm:flex-row gap-4 items-center justify-between">
            <div className="relative w-full sm:w-72">
              <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
                <Search className="h-4 w-4 text-text-muted" />
              </div>
              <Input
                placeholder="Search by customer name..."
                className="pl-9 w-full"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>
            <div className="w-full sm:w-auto flex items-center gap-2">
              <label className="text-sm font-medium text-text-secondary whitespace-nowrap">Status:</label>
              <select
                className="flex h-10 w-full rounded-md border border-border bg-surface px-3 py-2 text-sm text-text-primary focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value);
                  setPage(0); // Reset to page 0 on filter change
                }}
              >
                <option value="">All Orders</option>
                <option value="PENDING">Pending</option>
                <option value="ACCEPTED">Accepted</option>
                <option value="PREPARING">Preparing</option>
                <option value="READY">Ready</option>
                <option value="COMPLETED">Completed</option>
                <option value="CANCELLED">Cancelled</option>
              </select>
            </div>
          </div>
        </CardHeader>
        <CardContent className="p-0">
          {filteredOrders.length === 0 ? (
            <EmptyState
              title="No orders found"
              description={searchTerm ? "No orders match your search." : "You have no orders matching the selected status."}
              icon={<ShoppingBag className="h-8 w-8" />}
            />
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-sm text-left">
                <thead className="text-xs text-text-secondary uppercase bg-slate-50 border-b border-border">
                  <tr>
                    <th className="px-6 py-3 font-medium">Order ID</th>
                    <th className="px-6 py-3 font-medium">Customer</th>
                    <th className="px-6 py-3 font-medium">Status</th>
                    <th className="px-6 py-3 font-medium">Payment</th>
                    <th className="px-6 py-3 font-medium">Amount</th>
                    <th className="px-6 py-3 font-medium">Date</th>
                    <th className="px-6 py-3 font-medium text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {filteredOrders.map((order) => (
                    <tr key={order.orderId} className="hover:bg-slate-50/50 transition-colors">
                      <td className="px-6 py-4 font-mono text-xs text-text-secondary">
                        {order.orderId.substring(0, 8)}...
                      </td>
                      <td className="px-6 py-4 font-medium">{order.customerName}</td>
                      <td className="px-6 py-4">
                        <StatusChip status={getOrderStatusVariant(order.status)} label={order.status} />
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium
                          ${order.paymentStatus === 'COMPLETED' ? 'bg-green-100 text-green-800' : 
                            order.paymentStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800' : 
                            'bg-red-100 text-red-800'}`}>
                          {order.paymentStatus}
                        </span>
                      </td>
                      <td className="px-6 py-4 font-medium">
                        {new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(order.totalAmount)}
                      </td>
                      <td className="px-6 py-4 text-text-secondary">
                        {new Date(order.createdAt).toLocaleDateString()} {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </td>
                      <td className="px-6 py-4 text-right">
                        <Link href={`/vendor/orders/${order.orderId}`}>
                          <Button variant="ghost" size="sm" className="h-8 gap-1">
                            <Eye className="h-4 w-4" />
                            View
                          </Button>
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </CardContent>
      </Card>

      {data.totalPages > 1 && (
        <Pagination
          currentPage={page}
          totalPages={data.totalPages}
          onPageChange={setPage}
        />
      )}
    </div>
  );
}
