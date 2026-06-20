"use client";

import React, { useEffect } from "react";
import { createPortal } from "react-dom";
import { X } from "lucide-react";
import { cn } from "@/lib/utils";

export interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  description?: string;
  children: React.ReactNode;
  className?: string;
}

export function Modal({
  isOpen,
  onClose,
  title,
  description,
  children,
  className,
}: ModalProps) {
  // Prevent scrolling behind the modal when active
  useEffect(() => {
    if (isOpen) {
      document.body.style.overflow = "hidden";
    } else {
      document.body.style.overflow = "unset";
    }
    return () => {
      document.body.style.overflow = "unset";
    };
  }, [isOpen]);

  if (!isOpen) return null;

  // React Portal ensures the modal mounts directly at the document body level
  // to prevent relative positioning / overflow bugs in parents.
  const modalContent = (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      {/* Backdrop overlay */}
      <div
        className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs transition-opacity duration-300 animate-in fade-in"
        onClick={onClose}
      />

      {/* Modal Card Content Container */}
      <div
        role="dialog"
        aria-modal="true"
        className={cn(
          "relative z-10 w-full max-w-lg transform overflow-hidden rounded-2xl bg-surface p-6 shadow-2xl transition-all duration-300 ease-out animate-in fade-in zoom-in-95 border border-border text-text-primary",
          className
        )}
      >
        {/* Close Button */}
        <button
          onClick={onClose}
          className="absolute top-4 right-4 rounded-lg p-1.5 text-text-secondary hover:bg-muted hover:text-text-primary transition-colors focus-visible:outline-hidden"
          aria-label="Close modal"
        >
          <X className="h-5 w-5" />
        </button>

        {/* Title & Description */}
        <div className="mb-4">
          <h2 className="text-xl font-bold leading-tight">{title}</h2>
          {description && (
            <p className="mt-1.5 text-sm text-text-secondary font-medium">
              {description}
            </p>
          )}
        </div>

        {/* Modal content body */}
        <div className="mt-2">{children}</div>
      </div>
    </div>
  );

  return typeof document !== "undefined"
    ? createPortal(modalContent, document.body)
    : null;
}

export default Modal;
