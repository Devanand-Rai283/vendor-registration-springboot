"use client";

import React, { useState } from "react";
import { useVendorProfile, useVendorAnalytics } from "@/features/vendor/hooks/useVendorPortalQueries";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { BarChart3, TrendingUp, Calendar as CalendarIcon } from "lucide-react";

export default function VendorAnalyticsPage() {
  const [days, setDays] = useState(30);
  
  // First, get the profile to find the vendorId
  const { data: profile, isLoading: isProfileLoading, isError: isProfileError, error: profileError } = useVendorProfile();
  
  // Then, fetch analytics using that vendorId
  const { 
    data: analytics, 
    isLoading: isAnalyticsLoading, 
    isError: isAnalyticsError, 
    error: analyticsError,
    refetch 
  } = useVendorAnalytics(profile?.id, days);

  if (isProfileLoading || isAnalyticsLoading) {
    return <LoadingState message="Crunching the numbers..." />;
  }

  if (isProfileError || isAnalyticsError) {
    return (
      <ErrorState
        title="Failed to load analytics"
        error={isProfileError ? profileError : analyticsError}
        onRetry={() => refetch()}
      />
    );
  }

  if (!analytics || analytics.snapshots.length === 0) {
    return (
      <EmptyState
        title="No analytics data available"
        description="It looks like there is no data for the selected period."
        icon={<BarChart3 className="h-8 w-8" />}
      />
    );
  }

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(amount);
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString([], {
      weekday: 'short',
      month: 'short',
      day: 'numeric',
    });
  };

  const formatPeakHour = (hour?: number) => {
    if (hour === undefined || hour === null) return "N/A";
    const ampm = hour >= 12 ? 'PM' : 'AM';
    const hour12 = hour % 12 || 12;
    return `${hour12}:00 ${ampm}`;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-text-primary">Analytics & Trends</h1>
          <p className="text-text-secondary mt-1">Track your performance over the last {days} days.</p>
        </div>
        
        <div className="flex items-center gap-2 bg-surface border border-border rounded-lg p-1">
          <select
            className="bg-transparent text-sm font-medium px-3 py-1.5 focus:outline-none"
            value={days}
            onChange={(e) => setDays(Number(e.target.value))}
          >
            <option value={7}>Last 7 Days</option>
            <option value={14}>Last 14 Days</option>
            <option value={30}>Last 30 Days</option>
            <option value={90}>Last 90 Days</option>
          </select>
        </div>
      </div>

      <Card>
        <CardHeader className="border-b border-border bg-slate-50/50">
          <div className="flex items-center gap-2">
            <TrendingUp className="h-5 w-5 text-vendor-orange" />
            <CardTitle className="text-lg">Daily Performance Overview</CardTitle>
          </div>
          <CardDescription>
            Detailed tabular view of your store&apos;s key metrics.
          </CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-text-secondary uppercase bg-slate-50 border-b border-border">
                <tr>
                  <th className="px-6 py-4 font-medium flex items-center gap-1">
                    <CalendarIcon className="h-3 w-3" /> Date
                  </th>
                  <th className="px-6 py-4 font-medium text-right">Orders</th>
                  <th className="px-6 py-4 font-medium text-right">Revenue</th>
                  <th className="px-6 py-4 font-medium text-right">Avg Order Value</th>
                  <th className="px-6 py-4 font-medium">Top Item</th>
                  <th className="px-6 py-4 font-medium text-center">Peak Hour</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {analytics.snapshots.map((snapshot, index) => (
                  <tr key={snapshot.date || index} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4 font-medium text-text-primary">
                      {formatDate(snapshot.date)}
                    </td>
                    <td className="px-6 py-4 text-right font-medium">
                      {snapshot.totalOrders}
                    </td>
                    <td className="px-6 py-4 text-right font-medium text-green-700">
                      {formatCurrency(snapshot.totalRevenue)}
                    </td>
                    <td className="px-6 py-4 text-right text-text-secondary">
                      {formatCurrency(snapshot.averageOrderValue)}
                    </td>
                    <td className="px-6 py-4 text-text-secondary">
                      {snapshot.topMenuItemName || "N/A"}
                    </td>
                    <td className="px-6 py-4 text-center text-text-secondary">
                      {formatPeakHour(snapshot.peakHour)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
