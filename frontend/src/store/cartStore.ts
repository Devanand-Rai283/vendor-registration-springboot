import { create } from 'zustand';

export interface CartItem {
  menuItemId: string;
  name: string;
  price: number;
  quantity: number;
}

interface CartState {
  items: CartItem[];
  vendorId: string | null;
  notes: string;
  subtotal: number;
  itemCount: number;

  addItem: (item: CartItem, vendorId: string) => void;
  removeItem: (menuItemId: string) => void;
  updateQuantity: (menuItemId: string, quantity: number) => void;
  clearCart: () => void;
  setNotes: (notes: string) => void;
}

const calculateTotals = (items: CartItem[]) => {
  return items.reduce(
    (acc, item) => {
      acc.subtotal += item.price * item.quantity;
      acc.itemCount += item.quantity;
      return acc;
    },
    { subtotal: 0, itemCount: 0 }
  );
};

export const useCartStore = create<CartState>((set) => ({
  items: [],
  vendorId: null,
  notes: '',
  subtotal: 0,
  itemCount: 0,

  addItem: (item, vendorId) =>
    set((state) => {
      // Single vendor rule is enforced by the component before calling addItem
      // If vendorId is different, component should call clearCart first, or we replace it here
      // To be safe, if a different vendor is passed, we clear the cart
      let newItems = [...state.items];
      let newVendorId = state.vendorId;

      if (state.vendorId !== null && state.vendorId !== vendorId) {
        newItems = [];
        newVendorId = vendorId;
      } else if (state.vendorId === null) {
        newVendorId = vendorId;
      }

      const existingItemIndex = newItems.findIndex((i) => i.menuItemId === item.menuItemId);

      if (existingItemIndex >= 0) {
        newItems[existingItemIndex].quantity += item.quantity;
      } else {
        newItems.push(item);
      }

      const totals = calculateTotals(newItems);

      return {
        items: newItems,
        vendorId: newVendorId,
        subtotal: totals.subtotal,
        itemCount: totals.itemCount,
      };
    }),

  removeItem: (menuItemId) =>
    set((state) => {
      const newItems = state.items.filter((i) => i.menuItemId !== menuItemId);
      const totals = calculateTotals(newItems);
      return {
        items: newItems,
        vendorId: newItems.length === 0 ? null : state.vendorId,
        subtotal: totals.subtotal,
        itemCount: totals.itemCount,
        // Reset notes if cart is empty
        notes: newItems.length === 0 ? '' : state.notes,
      };
    }),

  updateQuantity: (menuItemId, quantity) =>
    set((state) => {
      if (quantity <= 0) {
        // Equivalent to remove
        const newItems = state.items.filter((i) => i.menuItemId !== menuItemId);
        const totals = calculateTotals(newItems);
        return {
          items: newItems,
          vendorId: newItems.length === 0 ? null : state.vendorId,
          subtotal: totals.subtotal,
          itemCount: totals.itemCount,
          notes: newItems.length === 0 ? '' : state.notes,
        };
      }

      const newItems = state.items.map((i) =>
        i.menuItemId === menuItemId ? { ...i, quantity } : i
      );
      const totals = calculateTotals(newItems);

      return {
        items: newItems,
        subtotal: totals.subtotal,
        itemCount: totals.itemCount,
      };
    }),

  clearCart: () =>
    set({
      items: [],
      vendorId: null,
      notes: '',
      subtotal: 0,
      itemCount: 0,
    }),

  setNotes: (notes) => set({ notes }),
}));
