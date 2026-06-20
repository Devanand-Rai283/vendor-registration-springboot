"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Compass, Store, LogOut, LayoutDashboard, ShoppingBag, BarChart3, FileText, UserCircle } from "lucide-react";
import { AuthGuard } from "@/features/auth/AuthGuard";
import { RoleGuard } from "@/features/auth/RoleGuard";
import { useAuthStore } from "@/store/authStore";
import { cn } from "@/lib/utils";

export default function VendorLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearAuth } = useAuthStore();

  const handleLogout = () => {
    clearAuth();
    router.push("/login");
  };

  const navItems = [
    { href: "/vendor", label: "Dashboard", icon: <LayoutDashboard className="h-4 w-4" /> },
    { href: "/vendor/orders", label: "Orders", icon: <ShoppingBag className="h-4 w-4" /> },
    { href: "/vendor/analytics", label: "Analytics", icon: <BarChart3 className="h-4 w-4" /> },
    { href: "/vendor/documents", label: "Documents", icon: <FileText className="h-4 w-4" /> },
    { href: "/vendor/profile", label: "Profile", icon: <UserCircle className="h-4 w-4" /> },
  ];

  return (
    <AuthGuard>
      <RoleGuard allowedRoles={["VENDOR"]}>
        <div className="flex min-h-screen flex-col bg-background">
          {/* Vendor Header */}
          <header className="sticky top-0 z-50 w-full border-b border-border bg-surface shadow-xs">
            <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between px-4 sm:px-6 lg:px-8">
              {/* Logo / Brand */}
              <div className="flex items-center gap-6">
                <Link href="/vendor" className="flex items-center gap-2 group">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-vendor-orange text-white shadow-lg transition-transform duration-300 group-hover:scale-105">
                    <Compass className="h-5 w-5" />
                  </div>
                  <span className="text-xl font-bold tracking-tight text-text-primary hidden sm:inline-block">
                    Street<span className="text-vendor-orange">Vendor</span>
                    <span className="ml-1.5 text-xs font-semibold px-2 py-0.5 rounded-full bg-orange-100 text-orange-700">Portal</span>
                  </span>
                </Link>

                {/* Navigation Links */}
                <nav className="hidden lg:flex items-center gap-1">
                  {navItems.map((item) => {
                    const isActive = pathname === item.href || (item.href !== "/vendor" && pathname.startsWith(item.href));
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={cn(
                          "flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                          isActive
                            ? "bg-vendor-orange/10 text-vendor-orange"
                            : "text-text-secondary hover:bg-muted hover:text-text-primary"
                        )}
                      >
                        {item.icon}
                        {item.label}
                      </Link>
                    );
                  })}
                </nav>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-4">
                <div className="hidden sm:flex flex-col text-right">
                  <span className="text-sm font-bold text-text-primary">{user?.name || "Vendor"}</span>
                  <span className="text-xs text-text-secondary">{user?.email}</span>
                </div>
                <button
                  onClick={handleLogout}
                  className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-surface text-text-secondary hover:bg-red-50 hover:text-red-600 hover:border-red-200 transition-all"
                  aria-label="Logout"
                  title="Sign out"
                >
                  <LogOut className="h-4 w-4" />
                </button>
              </div>
            </div>

            {/* Mobile Navigation */}
            <div className="lg:hidden flex overflow-x-auto border-t border-border px-4 py-2 gap-2 hide-scrollbar">
              {navItems.map((item) => {
                const isActive = pathname === item.href || (item.href !== "/vendor" && pathname.startsWith(item.href));
                return (
                  <Link
                    key={item.href}
                    href={item.href}
                    className={cn(
                      "flex whitespace-nowrap items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                      isActive
                        ? "bg-vendor-orange/10 text-vendor-orange"
                        : "text-text-secondary hover:bg-muted hover:text-text-primary"
                    )}
                  >
                    {item.icon}
                    {item.label}
                  </Link>
                );
              })}
            </div>
          </header>

          {/* Main Content Area */}
          <main className="flex-1 w-full max-w-[1440px] mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="animate-in fade-in slide-in-from-bottom-3 duration-500">
              {children}
            </div>
          </main>

          {/* Footer */}
          <footer className="border-t border-border bg-surface py-6">
            <div className="mx-auto max-w-[1440px] px-4 sm:px-6 lg:px-8 flex flex-col sm:flex-row items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <Store className="h-5 w-5 text-vendor-orange" />
                <span className="text-sm text-text-secondary font-medium">
                  Street Vendor Partner Portal &copy; {new Date().getFullYear()}
                </span>
              </div>
            </div>
          </footer>
        </div>
      </RoleGuard>
    </AuthGuard>
  );
}
