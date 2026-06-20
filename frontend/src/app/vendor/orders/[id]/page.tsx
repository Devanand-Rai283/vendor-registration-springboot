"use client";

import React from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useVendorOrderDetails } from "@/features/vendor/hooks/useVendorPortalQueries";
import { Card, CardContent, CardHeader, CardTitle, CardFooter } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { StatusChip } from "@/components/ui/status-chip";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { ArrowLeft, Phone, User, Calendar, FileText } from "lucide-react";

export default function VendorOrderDetailPage() {
  const params = useParams();
  const orderId = params.id as string;
  
  const { data: order, isLoading, isError, error, refetch } = useVendorOrderDetails(orderId);

  const getOrderStatusVariant = (status: string): "SUCCESS" | "WARNING" | "DANGER" | "INFO" => {
    switch (status) {
      case 'COMPLETED': return 'SUCCESS';
      case 'CANCELLED': return 'DANGER';
      case 'PENDING': return 'WARNING';
      default: return 'INFO';
    }
  };

  if (isLoading) {
    return <LoadingState message="Loading order details..." />;
  }

  if (isError || !order) {
    return (
      <ErrorState
        title="Order not found"
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString([], {
      weekday: 'short',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  return (
    <div className="space-y-6 max-w-5xl mx-auto">
      {/* Header & Back Navigation */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <Link href="/vendor/orders">
            <Button variant="outline" size="sm" className="h-9 px-3">
              <ArrowLeft className="h-4 w-4 mr-1" /> Back
            </Button>
          </Link>
          <div>
            <h1 className="text-2xl font-bold tracking-tight text-text-primary flex items-center gap-2">
              Order <span className="font-mono text-lg text-text-secondary">#{order.orderId.substring(0, 8)}</span>
            </h1>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <StatusChip status={getOrderStatusVariant(order.status)} label={order.status} />
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Left Column: Order Items */}
        <div className="md:col-span-2 space-y-6">
          <Card>
            <CardHeader className="border-b border-border bg-slate-50/50">
              <CardTitle className="text-lg">Order Items</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              <div className="overflow-x-auto">
                <table className="w-full text-sm text-left">
                  <thead className="text-xs text-text-secondary uppercase bg-slate-50 border-b border-border">
                    <tr>
                      <th className="px-6 py-3 font-medium">Item</th>
                      <th className="px-6 py-3 font-medium text-center">Qty</th>
                      <th className="px-6 py-3 font-medium text-right">Unit Price</th>
                      <th className="px-6 py-3 font-medium text-right">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {order.items.map((item) => (
                      <tr key={item.menuItemId} className="hover:bg-slate-50/50">
                        <td className="px-6 py-4 font-medium text-text-primary">
                          {item.itemName}
                        </td>
                        <td className="px-6 py-4 text-center font-medium">
                          {item.quantity}
                        </td>
                        <td className="px-6 py-4 text-right text-text-secondary">
                          {formatCurrency(item.unitPrice)}
                        </td>
                        <td className="px-6 py-4 text-right font-medium">
                          {formatCurrency(item.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </CardContent>
            <CardFooter className="flex flex-col items-end gap-2 bg-slate-50 border-t border-border p-6">
              <div className="flex justify-between w-full sm:w-64 text-sm text-text-secondary">
                <span>Subtotal</span>
                <span>{formatCurrency(order.totalAmount)}</span>
              </div>
              {/* If there are taxes or fees, they would go here */}
              <div className="flex justify-between w-full sm:w-64 text-lg font-bold text-text-primary mt-2 pt-2 border-t border-border">
                <span>Total</span>
                <span>{formatCurrency(order.totalAmount)}</span>
              </div>
            </CardFooter>
          </Card>

          {/* Notes Section */}
          {order.notes && (
            <Card>
              <CardHeader className="pb-3 border-b border-border bg-amber-50/30">
                <CardTitle className="text-sm flex items-center gap-2 text-amber-800">
                  <FileText className="h-4 w-4" /> Customer Notes
                </CardTitle>
              </CardHeader>
              <CardContent className="pt-4 text-sm whitespace-pre-wrap">
                {order.notes}
              </CardContent>
            </Card>
          )}
        </div>

        {/* Right Column: Customer & Details */}
        <div className="space-y-6">
          <Card>
            <CardHeader className="border-b border-border">
              <CardTitle className="text-sm">Customer Details</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 pt-4">
              <div className="flex items-start gap-3">
                <User className="h-4 w-4 text-text-secondary mt-0.5" />
                <div>
                  <p className="text-sm font-medium">{order.customerName}</p>
                  <p className="text-xs text-text-secondary font-mono mt-0.5">{order.customerId}</p>
                </div>
              </div>
              {order.customerPhone && (
                <div className="flex items-center gap-3">
                  <Phone className="h-4 w-4 text-text-secondary" />
                  <p className="text-sm">{order.customerPhone}</p>
                </div>
              )}
            </CardContent>
          </Card>

          <Card>
            <CardHeader className="border-b border-border">
              <CardTitle className="text-sm">Order Info</CardTitle>
            </CardHeader>
            <CardContent className="space-y-4 pt-4">
              <div className="flex items-start gap-3">
                <Calendar className="h-4 w-4 text-text-secondary mt-0.5" />
                <div>
                  <p className="text-sm font-medium">Placed At</p>
                  <p className="text-sm text-text-secondary mt-0.5">{formatDate(order.createdAt)}</p>
                </div>
              </div>
              <div className="flex flex-col gap-1 pt-2 border-t border-border">
                <p className="text-sm font-medium text-text-secondary">Payment Status</p>
                <div>
                  <span className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-medium
                    ${order.paymentStatus === 'COMPLETED' ? 'bg-green-100 text-green-800' : 
                      order.paymentStatus === 'PENDING' ? 'bg-yellow-100 text-yellow-800' : 
                      'bg-red-100 text-red-800'}`}>
                    {order.paymentStatus}
                  </span>
                </div>
              </div>
            </CardContent>
          </Card>

          {/* Quick Actions Placeholder */}
          <Card className="border-primary/20 bg-primary/5">
            <CardHeader className="pb-3">
              <CardTitle className="text-sm">Quick Actions</CardTitle>
            </CardHeader>
            <CardContent className="space-y-2">
              <p className="text-xs text-text-secondary mb-3">
                Change order status via the centralized tracking system (coming soon) or use your fulfillment console.
              </p>
              <Button className="w-full" variant="outline" disabled>
                Update Status
              </Button>
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  );
}
