"use client";

import React from "react";
import Link from "next/link";
import { Compass, ShieldCheck, User, ShoppingCart, ClipboardList } from "lucide-react";
import { useCartStore } from "@/store/cartStore";
import { CartDrawer } from "@/components/cart/CartDrawer";
import { useState } from "react";

interface PublicLayoutProps {
  children: React.ReactNode;
}

export default function PublicLayout({ children }: PublicLayoutProps) {
  const { itemCount } = useCartStore();
  const [cartOpen, setCartOpen] = useState(false);

  return (
    <div className="flex min-h-screen flex-col bg-background">
      {/* Navbar with flat Surface background */}
      <header className="sticky top-0 z-50 w-full border-b border-border bg-surface">
        <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between px-4 sm:px-6 lg:px-8">
          {/* Logo / Brand */}
          <Link href="/" className="flex items-center gap-2 group">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-street-blue text-white shadow-lg transition-transform duration-300 group-hover:scale-105 group-hover:rotate-3">
              <Compass className="h-5 w-5" />
            </div>
            <span className="text-xl font-bold tracking-tight text-text-primary">
              Street<span className="text-vendor-orange">Vendor</span>
            </span>
          </Link>

          {/* Navigation Links */}
          <nav className="hidden md:flex items-center gap-6">
            <Link href="/" className="text-sm font-medium text-text-primary hover:text-street-blue transition-colors">
              Discover
            </Link>
            <Link href="/vendors" className="text-sm font-medium text-text-secondary hover:text-street-blue transition-colors">
              Vendors
            </Link>
            <Link href="/orders" className="text-sm font-medium text-text-secondary hover:text-street-blue transition-colors flex items-center gap-1">
              <ClipboardList className="h-4 w-4" /> Orders
            </Link>
          </nav>

          {/* Action Buttons */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setCartOpen(true)}
              className="relative inline-flex h-9 w-9 items-center justify-center rounded-lg text-text-secondary hover:bg-muted transition-colors"
              aria-label="Open cart"
            >
              <ShoppingCart className="h-5 w-5" />
              {itemCount > 0 && (
                <span className="absolute -top-1 -right-1 flex h-4 w-4 items-center justify-center rounded-full bg-vendor-orange text-[10px] font-bold text-white border-2 border-surface">
                  {itemCount}
                </span>
              )}
            </button>
            <Link
              href="/register"
              className="inline-flex h-9 items-center justify-center rounded-lg border border-border bg-surface px-4 text-sm font-medium text-text-secondary shadow-xs hover:bg-muted hover:text-text-primary transition-all"
            >
              Partner Registration
            </Link>
            <Link
              href="/login"
              className="inline-flex h-9 items-center justify-center rounded-lg bg-street-blue px-4 text-sm font-medium text-white shadow-md hover:bg-blue-700 transition-all hover:scale-[1.02] active:scale-[0.98]"
            >
              <User className="mr-2 h-4 w-4" /> Sign In
            </Link>
          </div>
        </div>
      </header>

      {/* Main Content Area: Constrained to max-w-1440px */}
      <main className="flex-1 w-full max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
        <div className="animate-in fade-in slide-in-from-bottom-3 duration-500">
          {children}
        </div>
      </main>

      {/* Footer */}
      <footer className="border-t border-border bg-surface py-8">
        <div className="mx-auto max-w-[1440px] px-4 sm:px-6 lg:px-8 flex flex-col md:flex-row items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <ShieldCheck className="h-5 w-5 text-success" />
            <span className="text-sm text-text-secondary font-medium">
              Street Vendor Platform &copy; {new Date().getFullYear()} — Secure & Verified
            </span>
          </div>
          <div className="flex gap-6 text-sm text-text-secondary">
            <Link href="/terms" className="hover:text-street-blue transition-colors">Terms of Service</Link>
            <Link href="/privacy" className="hover:text-street-blue transition-colors">Privacy Policy</Link>
          </div>
        </div>
      </footer>

      <CartDrawer isOpen={cartOpen} onClose={() => setCartOpen(false)} />
    </div>
  );
}
