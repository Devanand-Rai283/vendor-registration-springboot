import { create } from "zustand";

interface UiState {
  sidebarOpen: boolean;
  theme: "light" | "dark";
  toggleSidebar: () => void;
  setSidebar: (open: boolean) => void;
  toggleTheme: () => void;
  setTheme: (theme: "light" | "dark") => void;
}

export const useUiStore = create<UiState>((set) => ({
  sidebarOpen: true,
  theme: "light",

  toggleSidebar: () => set((state) => ({ sidebarOpen: !state.sidebarOpen })),
  setSidebar: (open) => set({ sidebarOpen: open }),

  toggleTheme: () =>
    set((state) => {
      const nextTheme = state.theme === "light" ? "dark" : "light";
      if (typeof window !== "undefined") {
        const root = window.document.documentElement;
        root.classList.remove("light", "dark");
        root.classList.add(nextTheme);
      }
      return { theme: nextTheme };
    }),

  setTheme: (theme) =>
    set(() => {
      if (typeof window !== "undefined") {
        const root = window.document.documentElement;
        root.classList.remove("light", "dark");
        root.classList.add(theme);
      }
      return { theme };
    }),
}));
