"use client";

import React, { useState } from "react";
import { useOrderHistory, useCancelOrder } from "@/features/orders/hooks/useOrderQueries";
import { Container } from "@/components/layout/Container";
import { SectionHeader } from "@/components/layout/SectionHeader";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { StatusChip } from "@/components/ui/status-chip";
import { PaymentStatusBadge } from "@/components/payments/PaymentStatusBadge";
import { Button } from "@/components/ui/button";
import { Modal } from "@/components/ui/modal";
import { useToast } from "@/components/ui/toast";
import { ReviewModal } from "@/features/ratings/components/ReviewModal";

const STATUS_MAPPING: Record<string, { label: string; variant: "SUCCESS" | "WARNING" | "INFO" | "DANGER" }> = {
  PLACED: { label: "Pending", variant: "WARNING" },
  ACCEPTED: { label: "Confirmed", variant: "INFO" },
  PREPARING: { label: "Preparing", variant: "INFO" },
  READY: { label: "Ready", variant: "SUCCESS" },
  COMPLETED: { label: "Completed", variant: "SUCCESS" },
  CANCELLED: { label: "Cancelled", variant: "DANGER" },
};

export default function OrdersPage() {
  const [page, setPage] = useState(0);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [selectedOrderId, setSelectedOrderId] = useState<string | null>(null);
  
  const [reviewModalOpen, setReviewModalOpen] = useState(false);
  const [reviewOrderInfo, setReviewOrderInfo] = useState<{orderId: string, vendorId: string} | null>(null);

  const { addToast } = useToast();

  const { data, isLoading, error, refetch } = useOrderHistory(page, 20);
  const { mutate: cancelOrder, isPending: isCancelling } = useCancelOrder();

  const handleCancelClick = (orderId: string) => {
    setSelectedOrderId(orderId);
    setCancelModalOpen(true);
  };

  const handleRateClick = (orderId: string, vendorId: string) => {
    setReviewOrderInfo({ orderId, vendorId });
    setReviewModalOpen(true);
  };

  const confirmCancel = () => {
    if (!selectedOrderId) return;
    
    cancelOrder(selectedOrderId, {
      onSuccess: () => {
        addToast({
          title: "Order cancelled",
          description: "Your order has been cancelled successfully.",
          type: "success"
        });
        setCancelModalOpen(false);
        setSelectedOrderId(null);
      },
      onError: (err) => {
        addToast({
          title: "Failed to cancel order",
          description: err.message || "An unexpected error occurred.",
          type: "error"
        });
        setCancelModalOpen(false);
      }
    });
  };

  if (isLoading) {
    return (
      <Container className="py-8">
        <LoadingState message="Loading your order history..." />
      </Container>
    );
  }

  if (error) {
    return (
      <Container className="py-8">
        <ErrorState error={error} title="Failed to load orders" onRetry={() => refetch()} />
      </Container>
    );
  }

  const orders = data?.content || [];

  return (
    <div className="min-h-screen bg-gray-50 py-8">
      <Container>
        <SectionHeader title="My Orders" subtitle="View and manage your recent orders" />

        {orders.length === 0 ? (
          <EmptyState 
            title="No orders found" 
            description="You haven't placed any orders yet." 
          />
        ) : (
          <div className="mt-8 space-y-6">
            {orders.map((order) => {
              const statusInfo = STATUS_MAPPING[order.status] || { label: order.status, variant: "INFO" };
              const canCancel = order.status === "PLACED";

              return (
                <div key={order.orderId} className="bg-white p-6 rounded-xl border shadow-sm">
                  <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4 border-b pb-4 mb-4">
                    <div>
                      <h3 className="font-bold text-lg text-gray-900">{order.vendorBusinessName}</h3>
                      <p className="text-sm text-gray-500">Order #{order.orderId.substring(0, 8)}</p>
                    </div>
                    <div className="flex flex-col items-end gap-2">
                      <div className="flex gap-2">
                        <PaymentStatusBadge status={order.paymentStatus} />
                        <StatusChip status={statusInfo.variant} label={statusInfo.label} />
                      </div>
                      <span className="text-xs text-gray-500">
                        {new Date(order.createdAt).toLocaleDateString()} at {new Date(order.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                  </div>
                  
                  <div className="flex justify-between items-center">
                    <div>
                      <p className="text-sm text-gray-500">Total Amount</p>
                      <p className="font-bold text-gray-900">₹{order.totalAmount.toFixed(2)}</p>
                    </div>
                    
                    {canCancel && (
                      <Button 
                        variant="outline" 
                        size="sm"
                        className="text-red-600 hover:text-red-700 hover:bg-red-50 border-red-200"
                        onClick={() => handleCancelClick(order.orderId)}
                      >
                        Cancel Order
                      </Button>
                    )}
                    {order.status === "COMPLETED" && (
                      <Button 
                        variant="outline" 
                        size="sm"
                        className="text-street-blue hover:bg-street-blue/10 border-street-blue/30"
                        onClick={() => handleRateClick(order.orderId, order.vendorId)}
                      >
                        Rate Order
                      </Button>
                    )}
                  </div>
                </div>
              );
            })}

            {data && data.totalPages > 1 && (
              <div className="flex justify-center gap-2 mt-8">
                <Button 
                  variant="outline" 
                  disabled={page === 0} 
                  onClick={() => setPage(p => p - 1)}
                >
                  Previous
                </Button>
                <div className="flex items-center px-4">
                  <span className="text-sm text-gray-600">Page {page + 1} of {data.totalPages}</span>
                </div>
                <Button 
                  variant="outline" 
                  disabled={page >= data.totalPages - 1} 
                  onClick={() => setPage(p => p + 1)}
                >
                  Next
                </Button>
              </div>
            )}
          </div>
        )}
      </Container>

      <Modal
        isOpen={cancelModalOpen}
        onClose={() => setCancelModalOpen(false)}
        title="Cancel Order"
        description="Are you sure you want to cancel this order? This action cannot be undone."
      >
        <div className="flex justify-end gap-3 mt-6">
          <Button variant="outline" onClick={() => setCancelModalOpen(false)}>
            No, keep order
          </Button>
          <Button 
            variant="destructive" 
            onClick={confirmCancel}
            disabled={isCancelling}
          >
            {isCancelling ? 'Cancelling...' : 'Yes, cancel order'}
          </Button>
        </div>
      </Modal>

      {reviewOrderInfo && (
        <ReviewModal
          isOpen={reviewModalOpen}
          onClose={() => setReviewModalOpen(false)}
          orderId={reviewOrderInfo.orderId}
          vendorId={reviewOrderInfo.vendorId}
        />
      )}
    </div>
  );
}
