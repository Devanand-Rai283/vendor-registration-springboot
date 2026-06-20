import React from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";

interface AuthCardProps {
  children: React.ReactNode;
  title: string;
  description?: string;
}

export function AuthCard({ children, title, description }: AuthCardProps) {
  return (
    <div className="flex w-full max-w-md flex-col justify-center gap-6">
      <Card className="border border-border shadow-md bg-surface text-text-primary">
        <CardHeader className="space-y-2 text-center">
          <h1 className="text-2xl font-black tracking-tight">{title}</h1>
          {description && (
            <p className="text-sm text-text-secondary font-semibold">
              {description}
            </p>
          )}
        </CardHeader>
        <CardContent>{children}</CardContent>
      </Card>
    </div>
  );
}

export default AuthCard;
