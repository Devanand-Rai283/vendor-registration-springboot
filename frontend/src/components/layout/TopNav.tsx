"use client";

import React from "react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { cn } from "@/lib/utils";
import { Compass, Search, Store, Menu, X, ShoppingCart, ClipboardList } from "lucide-react";
import { useCartStore } from "@/store/cartStore";
import { CartDrawer } from "@/components/cart/CartDrawer";

interface NavLink {
  href: string;
  label: string;
  icon?: React.ReactNode;
}

interface TopNavProps {
  links?: NavLink[];
  actions?: React.ReactNode;
  className?: string;
}

const defaultLinks: NavLink[] = [
  { href: "/", label: "Home", icon: <Compass className="h-4 w-4" /> },
  { href: "/vendors", label: "Vendors", icon: <Store className="h-4 w-4" /> },
  { href: "/search", label: "Search", icon: <Search className="h-4 w-4" /> },
  { href: "/orders", label: "Orders", icon: <ClipboardList className="h-4 w-4" /> },
];

export function TopNav({
  links = defaultLinks,
  actions,
  className,
}: TopNavProps) {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = React.useState(false);
  const [cartOpen, setCartOpen] = React.useState(false);
  const { itemCount } = useCartStore();

  return (
    <header
      className={cn(
        "sticky top-0 z-50 w-full border-b border-border bg-surface/95 backdrop-blur-sm",
        className
      )}
    >
      <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between px-4 sm:px-6 lg:px-8">
        <Link href="/" className="flex items-center gap-2 group shrink-0">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-street-blue text-white shadow-sm transition-transform duration-300 group-hover:scale-105">
            <Compass className="h-4 w-4" />
          </div>
          <span className="text-lg font-bold tracking-tight text-text-primary">
            Street<span className="text-vendor-orange">Vendor</span>
          </span>
        </Link>

        <nav className="hidden md:flex items-center gap-1">
          {links.map((link) => {
            const isActive = pathname === link.href || (link.href !== "/" && pathname.startsWith(link.href));
            return (
              <Link
                key={link.href}
                href={link.href}
                className={cn(
                  "inline-flex items-center gap-2 rounded-lg px-3 py-2 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-muted text-street-blue"
                    : "text-text-secondary hover:bg-muted hover:text-text-primary"
                )}
              >
                {link.icon}
                {link.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-3">
          {actions}
          
          {/* Cart Indicator */}
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
          
          <button
            onClick={() => setMobileOpen(!mobileOpen)}
            className="md:hidden inline-flex h-9 w-9 items-center justify-center rounded-lg text-text-secondary hover:bg-muted"
            aria-label={mobileOpen ? "Close menu" : "Open menu"}
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {mobileOpen && (
        <nav className="md:hidden border-t border-border bg-surface p-4 space-y-1">
          {links.map((link) => {
            const isActive = pathname === link.href || (link.href !== "/" && pathname.startsWith(link.href));
            return (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setMobileOpen(false)}
                className={cn(
                  "flex items-center gap-2 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors",
                  isActive
                    ? "bg-muted text-street-blue"
                    : "text-text-secondary hover:bg-muted hover:text-text-primary"
                )}
              >
                {link.icon}
                {link.label}
              </Link>
            );
          })}
        </nav>
      )}

      <CartDrawer isOpen={cartOpen} onClose={() => setCartOpen(false)} />
    </header>
  );
}

export default TopNav;
