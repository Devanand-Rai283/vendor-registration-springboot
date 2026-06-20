"use client";

import React from "react";
import { useVendorDashboard } from "@/features/vendor/hooks/useVendorPortalQueries";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { Activity, ShoppingBag, DollarSign, TrendingUp, Star, Users } from "lucide-react";

export default function VendorDashboardPage() {
  const { data: metrics, isLoading, isError, error, refetch } = useVendorDashboard();

  if (isLoading) {
    return <LoadingState message="Loading dashboard metrics..." />;
  }

  if (isError || !metrics) {
    return (
      <ErrorState
        title="Failed to load metrics"
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  const kpiCards = [
    {
      title: "Active Orders",
      value: metrics.activeOrders,
      icon: <Activity className="h-5 w-5 text-blue-500" />,
      description: "Orders currently in progress",
    },
    {
      title: "Total Orders",
      value: metrics.totalOrders,
      icon: <ShoppingBag className="h-5 w-5 text-vendor-orange" />,
      description: "All-time orders",
    },
    {
      title: "Total Revenue",
      value: new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(metrics.totalRevenue),
      icon: <DollarSign className="h-5 w-5 text-green-500" />,
      description: "All-time earnings",
    },
    {
      title: "Avg. Order Value",
      value: new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(metrics.averageOrderValue),
      icon: <TrendingUp className="h-5 w-5 text-purple-500" />,
      description: "Revenue per order",
    },
    {
      title: "Average Rating",
      value: metrics.averageRating.toFixed(1),
      icon: <Star className="h-5 w-5 text-yellow-500" />,
      description: `Based on ${metrics.totalReviews} reviews`,
    },
    {
      title: "Total Reviews",
      value: metrics.totalReviews,
      icon: <Users className="h-5 w-5 text-indigo-500" />,
      description: "Customer feedback received",
    },
  ];

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight text-text-primary">Dashboard Overview</h1>
        <p className="text-text-secondary mt-1">Here is a summary of your store&apos;s performance.</p>
      </div>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {kpiCards.map((kpi, index) => (
          <Card key={index} className="overflow-hidden">
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">{kpi.title}</CardTitle>
              <div className="p-2 bg-slate-100 rounded-full">{kpi.icon}</div>
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{kpi.value}</div>
              <p className="text-xs text-text-secondary mt-1">{kpi.description}</p>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
