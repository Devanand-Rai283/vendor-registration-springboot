# Vendor Domain Skill

## Purpose

Preserve the business rules governing vendors within the Street Vendor Platform.

---

## When to Use

Activate whenever:

* Implementing vendor features
* Modifying vendor workflows
* Designing discovery logic
* Reviewing vendor-related pull requests

---

## Vendor Lifecycle

Vendor Journey:

Register Account

↓

Create Vendor Profile

↓

Upload Documents

↓

Await Review

↓

Approved or Rejected

↓

Manage Menu

↓

Receive Orders

↓

View Analytics

---

## Vendor Status Rules

Statuses:

PENDING_REVIEW

APPROVED

REJECTED

---

### PENDING_REVIEW

Vendor CAN:

* View dashboard
* Check approval status at any time via `GET /api/vendors/me` — this endpoint is available to the authenticated vendor regardless of approval state and returns the current `status` field (PENDING_REVIEW / APPROVED / REJECTED). This is the designed polling mechanism; there are no push notifications in MVP.

Vendor CANNOT:

* Receive orders
* Appear in discovery
* Manage active business operations

---

### APPROVED

Vendor CAN:

* Manage menus
* Receive orders
* Update order statuses
* Access analytics dashboard (snapshot-based summary — see Analytics Rules for exact scope)

Vendor appears in:

* Nearby discovery
* Search results

---

### REJECTED

Vendor CAN:

* View rejection reason

Vendor CANNOT:

* Receive orders
* Manage menus
* Participate in discovery

---

## Vendor Ownership Rules

A vendor may access only:

* Their own profile
* Their own menu
* Their own documents
* Their own orders
* Their own analytics

Ownership validation is mandatory.

Role checks alone are insufficient.

---

## Discovery Rules

Only APPROVED vendors appear in:

* Nearby vendor search
* Food search results

---

## Menu Rules

Vendor may:

* Create categories
* Create items
* Update items
* Toggle availability

Availability changes must be reflected immediately.

---

## Order Rules

### Single-Vendor Constraint

Customers may only order from one vendor per order. An order cannot contain items from multiple vendors.

This rule is enforced at two independent layers:

**Frontend layer (FSD §12 — cart conflict modal):**

When a customer attempts to add an item from a different vendor than the one already in the cart, the frontend must show a conflict modal before making any API call:

* Title: "Start a new cart?"
* Body: "Your cart has items from [Vendor A]. Adding items from [Vendor B] will clear your current cart."
* Primary action: "Clear cart and add" → clears cart, adds new item
* Secondary action: "Keep current cart" → dismisses modal, item not added

This modal fires before the API call — not after a 400 response. The purpose is UX: avoid a round-trip to surface an error the frontend can detect locally.

**Backend layer (ORDER-002 — POST /api/orders):**

`POST /api/orders` must independently validate that all items in the request body belong to the same vendor. If items from multiple vendors are detected, return `400 Bad Request`. Do not trust the frontend to have enforced this.

Both layers are required. The backend check is the authoritative enforcement. The frontend check is the user experience optimization.

---

### Vendor Order Status Transitions

Vendor may drive the following transitions:

PLACED

↓

ACCEPTED or CANCELLED (vendor-initiated cancellation)

↓

PREPARING

↓

READY

↓

COMPLETED

Invalid transitions must be rejected with 400 Bad Request.

Every valid transition must be written to AUDIT_LOGS as ORDER_STATUS_CHANGED.

---

## Analytics Rules

### Scope Authority

This section is the definitive scope boundary for analytics across all tickets and planning decisions.

If the scope in any other document (FSD, PRD, TAD) appears to conflict with this section, STOP and raise the conflict before implementing. Do not resolve scope conflicts silently.

---

### What Analytics IS in MVP

Analytics is a snapshot-based summary system. It answers: "How did this vendor perform yesterday and over the last N days?"

**Backend (ANALYTICS-001 — SHOULD HAVE):**

A Spring `@Scheduled` job runs daily at 01:00 (configurable via `ANALYTICS_CRON_EXPRESSION`).

For each vendor with at least one completed paid order, it writes one record to `ANALYTICS_SNAPSHOTS`:

* `total_orders` — count of PAID orders for that day
* `total_revenue` — sum of payment amounts for that day
* `average_order_value` — total_revenue / total_orders
* `top_item_id` — most frequently ordered menu item
* `peak_hour` — hour of day (0–23) with most orders

Upsert on `(vendor_id, snapshot_date)` — re-running the job for the same date updates the existing record, never creates a duplicate.

**API (ANALYTICS-002 — SHOULD HAVE):**

`GET /api/vendors/{id}/analytics?days=30` (default 30, max 90)

Returns an array of daily snapshot records ordered by `snapshot_date ASC`.

Response shape:

```json
{
  "vendorId": "...",
  "snapshots": [
    {
      "snapshotDate": "2026-06-01",
      "totalOrders": 24,
      "totalRevenue": 12000,
      "averageOrderValue": 500,
      "topItem": "Samosa",
      "peakHour": 18
    }
  ],
  "periodDays": 30
}
```

Returns `{ "snapshots": [] }` for new vendors with no data. Never returns 404 for missing snapshots.

**Caching:**

After each snapshot write, update Redis key `analytics:{vendorId}` with TTL 900s.

API reads from Redis first; falls back to DB on cache miss and re-populates Redis.

**Access:**

* Vendor: own analytics only (`analytics.vendor_id == authenticated vendor`)
* Admin: any vendor's analytics (bypasses ownership check — see ANALYTICS-002)
* Customer: no access

---

### What Analytics IS NOT in MVP

These are out of scope for ANALYTICS-001 and ANALYTICS-002. Do not implement them in MVP tickets regardless of what the FSD charts section shows:

* Real-time order counts or live revenue figures
* Trend charts rendered in the vendor dashboard (Revenue Trend, Orders Trend from FSD §13) — the backend API supports them but the frontend chart components are SHOULD HAVE and ship only after ANALYTICS-002 backend is verified complete
* Multi-period comparisons (e.g., this week vs last week)
* Cross-vendor analytics or platform-wide aggregates (that is Admin-only platform monitoring, not vendor analytics)
* Revenue forecasting
* AI-driven recommendations

---

### Phase Boundary

| Feature | Ticket | Priority | Phase |
|---|---|---|---|
| Daily snapshot job | ANALYTICS-001 | SHOULD HAVE | MVP |
| Analytics summary API | ANALYTICS-002 | SHOULD HAVE | MVP |
| Frontend trend charts (Revenue, Orders) | No ticket yet | SHOULD HAVE | MVP — only after ANALYTICS-002 is shipped |
| Advanced multi-period dashboard | Post-MVP | NICE TO HAVE | Phase 2 |
| Revenue forecasting | Post-MVP | NICE TO HAVE | Phase 2 |

ANALYTICS-001 and ANALYTICS-002 do not block the core transaction flow (Auth → Vendor → Menu → Orders → Payments). They may ship in parallel or after.

---

### Data Source

Aggregations must query `ORDERS` + `ORDER_ITEMS` + `PAYMENTS` where `PAYMENTS.status = PAID`.

Do not aggregate orders with payment_status = PENDING or FAILED.

---

## Validation Checklist

✓ Vendor ownership enforced.

✓ Approval status respected.

✓ Discovery excludes unapproved vendors.

✓ Order transitions validated.

✓ Analytics access restricted (vendor sees own data only; admin bypasses ownership).

✓ Analytics scope is snapshot-based summary — no real-time data, no trend chart components built before ANALYTICS-002 ships.

✓ Vendor isolation maintained.

---

## Anti-Patterns

Avoid:

* Exposing rejected vendors publicly.

* Allowing vendors to manage other vendors' resources.

* Skipping approval checks.

* Allowing invalid order transitions.

* Treating role checks as ownership checks.

* Building FSD §13 trend chart frontend components (Revenue Trend, Orders Trend) before ANALYTICS-002 backend is verified complete and deployed.

* Implementing real-time analytics (live order counts, streaming revenue) in MVP — the snapshot job runs once daily; there is no real-time analytics in Phase 1.

* Expanding ANALYTICS-001 or ANALYTICS-002 scope to include multi-period comparisons, cross-vendor data, or forecasting — those are explicitly Phase 2.

* Treating the PRD "Should Have" priority for analytics as permission to skip it entirely — ANALYTICS-001 and ANALYTICS-002 must ship before the vendor analytics dashboard screens are enabled.
