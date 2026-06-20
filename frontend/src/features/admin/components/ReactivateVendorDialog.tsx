import React from "react";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { useReactivateVendor } from "../hooks/useAdminQueries";
import { useToast } from "@/components/ui/toast";
import { ShieldCheck, Loader2 } from "lucide-react";

interface ReactivateVendorDialogProps {
  isOpen: boolean;
  onClose: () => void;
  vendorId: string;
  businessName: string;
}

export function ReactivateVendorDialog({
  isOpen,
  onClose,
  vendorId,
  businessName,
}: ReactivateVendorDialogProps) {
  const { mutate: reactivateVendor, isPending } = useReactivateVendor();
  const { addToast } = useToast();

  const handleConfirm = () => {
    reactivateVendor(vendorId, {
      onSuccess: () => {
        addToast({
          title: "Account Reactivated",
          description: `Successfully reactivated ${businessName || "vendor"}.`,
          type: "success",
        });
        onClose();
      },
      onError: (err: unknown) => {
        const message = err instanceof Error ? err.message : "Failed to reactivate the vendor account.";
        addToast({
          title: "Reactivation Failed",
          description: message,
          type: "error",
        });
      },
    });
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Reactivate Vendor Account"
      className="max-w-md"
    >
      <div className="flex flex-col items-center text-center p-2">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-success/10 text-success mb-4">
          <ShieldCheck className="h-6 w-6" />
        </div>
        <p className="text-sm text-text-secondary leading-relaxed">
          Are you sure you want to reactivate <strong className="text-text-primary">{businessName}</strong>?
        </p>
        <p className="mt-2 text-xs text-text-secondary">
          This will restore user login access, remove active Redis lockouts, and reactivate the vendor account status.
        </p>
      </div>

      <div className="flex justify-end gap-3 mt-6">
        <Button variant="outline" onClick={onClose} disabled={isPending}>
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          disabled={isPending}
          className="min-w-[100px] bg-street-blue text-white hover:bg-blue-600"
        >
          {isPending ? (
            <span className="flex items-center justify-center gap-1.5">
              <Loader2 className="h-4 w-4 animate-spin" />
              Reactivating
            </span>
          ) : (
            "Reactivate"
          )}
        </Button>
      </div>
    </Modal>
  );
}
