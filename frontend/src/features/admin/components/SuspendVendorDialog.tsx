import React from "react";
import { Modal } from "@/components/ui/modal";
import { Button } from "@/components/ui/button";
import { useSuspendVendor } from "../hooks/useAdminQueries";
import { useToast } from "@/components/ui/toast";
import { ShieldAlert, Loader2 } from "lucide-react";

interface SuspendVendorDialogProps {
  isOpen: boolean;
  onClose: () => void;
  vendorId: string;
  businessName: string;
}

export function SuspendVendorDialog({
  isOpen,
  onClose,
  vendorId,
  businessName,
}: SuspendVendorDialogProps) {
  const { mutate: suspendVendor, isPending } = useSuspendVendor();
  const { addToast } = useToast();

  const handleConfirm = () => {
    suspendVendor(vendorId, {
      onSuccess: () => {
        addToast({
          title: "Account Suspended",
          description: `Successfully suspended ${businessName || "vendor"}.`,
          type: "success",
        });
        onClose();
      },
      onError: (err: unknown) => {
        const message = err instanceof Error ? err.message : "Failed to suspend the vendor account.";
        addToast({
          title: "Suspension Failed",
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
      title="Suspend Vendor Account"
      className="max-w-md"
    >
      <div className="flex flex-col items-center text-center p-2">
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-danger/10 text-danger mb-4">
          <ShieldAlert className="h-6 w-6" />
        </div>
        <p className="text-sm text-text-secondary leading-relaxed">
          Are you sure you want to suspend <strong className="text-text-primary">{businessName}</strong>?
        </p>
        <p className="mt-2 text-xs text-danger font-medium">
          Warning: This will immediately invalidate all active sessions, lock out the user, and prevent access to the platform.
        </p>
      </div>

      <div className="flex justify-end gap-3 mt-6">
        <Button variant="outline" onClick={onClose} disabled={isPending}>
          Cancel
        </Button>
        <Button
          variant="destructive"
          onClick={handleConfirm}
          disabled={isPending}
          className="min-w-[100px]"
        >
          {isPending ? (
            <span className="flex items-center justify-center gap-1.5">
              <Loader2 className="h-4 w-4 animate-spin" />
              Suspending
            </span>
          ) : (
            "Suspend"
          )}
        </Button>
      </div>
    </Modal>
  );
}
