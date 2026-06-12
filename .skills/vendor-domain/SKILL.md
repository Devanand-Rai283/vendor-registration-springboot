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
* Check approval status

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
* Access analytics

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

Vendor may:

PLACED

↓

ACCEPTED or CANCELLED

↓

PREPARING

↓

READY

↓

COMPLETED

Invalid transitions must be rejected.

---

## Analytics Rules

Vendor analytics are private.

Vendors may view only their own data.

---

## Validation Checklist

✓ Vendor ownership enforced.

✓ Approval status respected.

✓ Discovery excludes unapproved vendors.

✓ Order transitions validated.

✓ Analytics access restricted.

✓ Vendor isolation maintained.

---

## Anti-Patterns

Avoid:

* Exposing rejected vendors publicly.

* Allowing vendors to manage other vendors' resources.

* Skipping approval checks.

* Allowing invalid order transitions.

* Treating role checks as ownership checks.
