import type { Metadata } from "next";
import { fontInter } from "@/lib/fonts";
import { Providers } from "@/app/providers";
import "./globals.css";
import { cn } from "@/lib/utils";

export const metadata: Metadata = {
  title: "Street Vendor Platform | Discover Local Food & Services",
  description:
    "Explore, discover, and support local street vendors in your community. A modern platform for local food stalls, food trucks, and micro-entrepreneurs.",
  keywords: ["street food", "local vendors", "food trucks", "community commerce", "vendor registration"],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className={cn("scroll-smooth", fontInter.variable)}>
      <body className="min-h-screen bg-background font-sans antialiased text-text-primary">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
