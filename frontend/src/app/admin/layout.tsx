"use client";

import React from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Compass, ShieldCheck, LogOut, Store, LayoutDashboard } from "lucide-react";
import { AuthGuard } from "@/features/auth/AuthGuard";
import { RoleGuard } from "@/features/auth/RoleGuard";
import { useAuthStore } from "@/store/authStore";
import { cn } from "@/lib/utils";

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, clearAuth } = useAuthStore();

  const handleLogout = () => {
    clearAuth();
    router.push("/login");
  };

  const navItems = [
    { href: "/admin", label: "Dashboard", icon: <LayoutDashboard className="h-4 w-4" /> },
    { href: "/admin/vendors", label: "Vendors", icon: <Store className="h-4 w-4" /> },
  ];

  return (
    <AuthGuard>
      <RoleGuard allowedRoles={["ADMIN"]}>
        <div className="flex min-h-screen flex-col bg-background">
          {/* Admin Header */}
          <header className="sticky top-0 z-50 w-full border-b border-border bg-surface shadow-xs">
            <div className="mx-auto flex h-16 max-w-[1440px] items-center justify-between px-4 sm:px-6 lg:px-8">
              {/* Logo / Brand */}
              <div className="flex items-center gap-6">
                <Link href="/admin" className="flex items-center gap-2 group">
                  <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-slate-900 text-white shadow-lg transition-transform duration-300 group-hover:scale-105">
                    <Compass className="h-5 w-5" />
                  </div>
                  <span className="text-xl font-bold tracking-tight text-text-primary">
                    Street<span className="text-vendor-orange">Vendor</span>
                    <span className="ml-1.5 text-xs font-semibold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">Admin</span>
                  </span>
                </Link>

                {/* Navigation Links */}
                <nav className="hidden md:flex items-center gap-1">
                  {navItems.map((item) => {
                    const isActive = pathname === item.href || (item.href !== "/admin" && pathname.startsWith(item.href));
                    return (
                      <Link
                        key={item.href}
                        href={item.href}
                        className={cn(
                          "flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium transition-colors",
                          isActive
                            ? "bg-slate-900 text-white"
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
                  <span className="text-sm font-bold text-text-primary">{user?.name || "Administrator"}</span>
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
                <ShieldCheck className="h-5 w-5 text-success" />
                <span className="text-sm text-text-secondary font-medium">
                  Street Vendor Admin Console &copy; {new Date().getFullYear()} — Secure Access
                </span>
              </div>
            </div>
          </footer>
        </div>
      </RoleGuard>
    </AuthGuard>
  );
}
