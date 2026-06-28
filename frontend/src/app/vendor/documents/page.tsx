"use client";

import React from "react";
import { useVendorDocuments } from "@/features/vendor/hooks/useVendorPortalQueries";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { LoadingState } from "@/components/ui/loading-state";
import { ErrorState } from "@/components/ui/error-state";
import { EmptyState } from "@/components/ui/empty-state";
import { FileText, ShieldCheck, Clock, XCircle, ExternalLink, AlertCircle } from "lucide-react";

export default function VendorDocumentsPage() {
  const { data: documents, isLoading, isError, error, refetch } = useVendorDocuments();

  if (isLoading) {
    return <LoadingState message="Loading your documents..." />;
  }

  if (isError || !documents) {
    return (
      <ErrorState
        title="Failed to load documents"
        error={error}
        onRetry={() => refetch()}
      />
    );
  }

  if (documents.length === 0) {
    return (
      <div className="space-y-6">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-text-primary">Document Center</h1>
          <p className="text-text-secondary mt-1">Manage your official verification documents.</p>
        </div>
        <EmptyState
          title="No documents uploaded"
          description="You haven't uploaded any verification documents yet. Upload them from the setup portal."
          icon={<FileText className="h-8 w-8" />}
        />
      </div>
    );
  }

  const formatDocType = (type: string) => {
    return type.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ');
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'VERIFIED': return <ShieldCheck className="h-4 w-4 text-green-600" />;
      case 'PENDING': return <Clock className="h-4 w-4 text-amber-600" />;
      case 'REJECTED': return <XCircle className="h-4 w-4 text-red-600" />;
      default: return <Clock className="h-4 w-4 text-slate-600" />;
    }
  };

  const getStatusBadgeClass = (status: string) => {
    switch (status) {
      case 'VERIFIED': return 'bg-green-100 text-green-800 border border-green-200';
      case 'PENDING': return 'bg-amber-100 text-amber-800 border border-amber-200';
      case 'REJECTED': return 'bg-red-100 text-red-800 border border-red-200';
      default: return 'bg-slate-100 text-slate-800 border border-slate-200';
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-text-primary">Document Center</h1>
          <p className="text-text-secondary mt-1">Manage your official verification documents.</p>
        </div>
      </div>

      <Card>
        <CardHeader className="border-b border-border">
          <CardTitle className="text-lg">Uploaded Documents</CardTitle>
          <CardDescription>View the status of your business and food safety verifications.</CardDescription>
        </CardHeader>
        <CardContent className="p-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm text-left">
              <thead className="text-xs text-text-secondary uppercase bg-slate-50 border-b border-border">
                <tr>
                  <th className="px-6 py-3 font-medium">Document Type</th>
                  <th className="px-6 py-3 font-medium">Status</th>
                  <th className="px-6 py-3 font-medium">Uploaded Date</th>
                  <th className="px-6 py-3 font-medium text-right">Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {documents.map((doc) => (
                  <tr key={doc.documentId} className="hover:bg-slate-50/50 transition-colors">
                    <td className="px-6 py-4 font-medium text-text-primary">
                      <div className="flex items-center gap-2">
                        <FileText className="h-4 w-4 text-slate-400" />
                        {formatDocType(doc.documentType)}
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <span className={`inline-flex items-center gap-1.5 rounded-full px-2.5 py-1 text-xs font-medium ${getStatusBadgeClass(doc.verificationStatus)}`}>
                        {getStatusIcon(doc.verificationStatus)}
                        {doc.verificationStatus.charAt(0) + doc.verificationStatus.slice(1).toLowerCase()}
                      </span>
                    </td>
                    <td className="px-6 py-4 text-text-secondary">
                      {new Date(doc.uploadedAt).toLocaleDateString()}
                    </td>
                    <td className="px-6 py-4 text-right">
                      {doc.viewUrl ? (
                        <Button
                          variant="outline"
                          size="sm"
                          className="h-8 gap-1.5"
                          onClick={() => window.open(doc.viewUrl!, "_blank")}
                        >
                          View <ExternalLink className="h-3.5 w-3.5" />
                        </Button>
                      ) : (
                        <div className="inline-flex items-center justify-end w-full text-xs text-amber-600 gap-1 font-medium bg-amber-50 px-2.5 py-1.5 rounded border border-amber-100">
                          <AlertCircle className="h-3.5 w-3.5" />
                          Unavailable
                        </div>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </CardContent>
      </Card>

      <div className="bg-blue-50 border border-blue-100 rounded-lg p-4 flex gap-3 text-sm text-blue-800">
        <ShieldCheck className="h-5 w-5 text-blue-600 shrink-0" />
        <p>
          Your documents are securely stored. Temporary view links expire after 15 minutes.
          If a document shows as <span className="font-semibold text-amber-700">Unavailable</span>, our secure storage service may be temporarily degraded. Please try again later.
        </p>
      </div>
    </div>
  );
}
