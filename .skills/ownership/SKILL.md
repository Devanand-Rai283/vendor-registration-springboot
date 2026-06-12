# Ownership Skill

## Purpose

Enforce resource ownership throughout the platform.

Role checks alone are insufficient.

Ownership validation is mandatory.

---

## When to Use

Activate whenever implementing:

* Orders
* Menus
* Vendor features
* Customer features
* Admin functionality
* Analytics access
* File access

---

## Ownership Principle

The authenticated user may interact only with resources they own or are authorized to manage.

Always validate ownership.

Never assume ownership from role alone.

---

## Customer Ownership Rules

Customers MAY access:

* Their own profile
* Their own orders
* Their own reviews

Customers MAY NOT access:

* Other customers' orders
* Vendor dashboards
* Vendor documents
* Admin functionality

---

## Vendor Ownership Rules

Vendors MAY access:

* Their own vendor profile
* Their own menu items
* Their own categories
* Their own orders
* Their own analytics
* Their own documents

Vendors MAY NOT access:

* Other vendors' resources
* Admin functionality
* Customer records unrelated to orders

---

## Administrator Rules

Administrators MAY:

* Approve vendors
* Reject vendors
* Suspend accounts
* Reactivate accounts
* View platform metrics

Administrators MAY NOT:

* Access passwords
* Impersonate users
* Modify payment outcomes
* Rewrite historical events

---

## Validation Strategy

Role Check

↓

Ownership Check

↓

Business Rule Validation

All three must pass.

---

## Order Ownership Rules

Customer:

order.customer_id == authenticated customer

Vendor:

order.vendor_id == authenticated vendor

Admin:

unrestricted access

---

## Menu Ownership Rules

menu_item.vendor_id == authenticated vendor

Required for:

* Updates
* Deletes
* Availability changes

---

## Document Ownership Rules

Vendor:

Own documents only

Admin:

All documents

Customer:

No access

---

## Analytics Ownership Rules

Vendor:

Own analytics only

Admin:

Platform analytics only

Customer:

No access

---

## Validation Checklist

✓ Role validated.

✓ Ownership validated.

✓ Business rules validated.

✓ Cross-tenant access prevented.

✓ Unauthorized requests return 403.

---

## Anti-Patterns

Avoid:

* Role-only authorization.

* Trusting request parameters.

* Exposing unrelated records.

* Skipping ownership checks during updates.

* Returning partial sensitive data.
