"use client";

import React, { Component, ErrorInfo, ReactNode } from "react";
import { AlertOctagon } from "lucide-react";

interface Props {
  children?: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("ErrorBoundary caught an uncaught error:", error, errorInfo);
  }

  public handleReset = () => {
    this.setState({ hasError: false, error: null });
    if (typeof window !== "undefined") {
      window.location.reload();
    }
  };

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div className="flex min-h-[400px] flex-col items-center justify-center rounded-2xl border border-red-100 bg-red-50/30 p-8 text-center backdrop-blur-xs">
          <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-danger text-white shadow-md animate-bounce">
            <AlertOctagon className="h-6 w-6" />
          </div>
          <h2 className="mt-4 text-xl font-bold text-text-primary">Something went wrong</h2>
          <p className="mt-2 text-sm text-text-secondary max-w-md">
            An unexpected error occurred during page rendering. Our developers have been notified.
          </p>
          {this.state.error && (
            <div className="mt-4 max-w-lg overflow-x-auto rounded-lg bg-slate-900 p-3 text-left text-xs text-red-400 font-mono">
              {this.state.error.toString()}
            </div>
          )}
          <button
            onClick={this.handleReset}
            className="mt-6 inline-flex h-10 items-center justify-center rounded-lg bg-street-blue px-6 text-sm font-semibold text-white shadow-md hover:bg-blue-700 transition-colors"
          >
            Reload Page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}
