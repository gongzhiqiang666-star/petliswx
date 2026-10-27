# Confirmed Mini-program Merge Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Preserve the confirmed 38-page mini-program and existing MySQL schema while delivering the full Excel feature set in one Java backend and Web admin.

**Architecture:** Extend `petlis-backend` as the sole application and existing `lin_*` tables as sole masters. Port modules from `/Users/apple/claudelife/petlis/backend` and `admin`, using additive-only new workflow tables and an admin response adapter.

**Tech Stack:** Java 21, Spring Boot 3.4, JPA/JdbcTemplate, MySQL 8.4, JUnit 5, native ES Modules, native WeChat JavaScript/WXML/WXSS, Node test runner, Playwright.

**Spec:** `docs/superpowers/specs/2026-10-27-confirmed-miniprogram-merge-design.md`

## Global Constraints

- Never modify/replay/backfill existing tables, triggers or migrations; no second business database or synonymous master tables.
- Execute new DDL only on an isolated MySQL copy, never the Mac mini source database.
- Preserve mini-program `code=200`, `/api`, zero-based pages, existing IDs/statuses, two pagination shapes and yuan API amounts.
- Keep all 38 confirmed pages, five tabs, components, assets and workflows; no real payment, only mock/synthetic tests.
- Commits and PR content are Chinese; PR sections are `概述/验证/风险`.

## Review Focus

- Existing schema before/after snapshots must match; Task 1 and Task 6 run automated checks.
- Old order/report IDs and mini-program response/pagination shapes must not change; Tasks 1, 3 and 6 pin contracts.
- New payment, refund, bill and redemption writes must be idempotent and ownership checked; Tasks 4 and 6 test repeats and foreign hospitals.
- Report publish must synchronize legacy detail/order and enforce payment; Tasks 3 and 6 test both.
- New modules must not duplicate legacy notifications or masters; Tasks 3, 4 and 6 assert single records/events.

---

### Task 1: Contract Baseline and Schema Guard

**Files:** `petlis-backend/src/test/java/com/petlis/contract/**`, `petlis-backend/src/test/java/com/petlis/schema/**`, `petlis-backend/src/main/resources/db/platform/**`, `docs/FUNCTION-MATRIX.md`, `integration/**`.
**Interfaces:** reusable legacy API fixtures and schema snapshot assertion; additive platform migration disabled by default.

- [ ] Add failing contract tests for legacy success code, page shapes and key login/pet/order/report flows; run and record baseline.
- [ ] Add schema snapshot guard and Excel feature matrix with implementation/verification owners.
- [ ] Create only required new-table DDL with no ALTER/DROP/TRUNCATE; validate on isolated MySQL and assert legacy schema unchanged.
- [ ] Run all existing backend tests and mini-program syntax/contract checks.

### Task 2: Unified Roles and Web Admin Foundation

**Files:** `petlis-backend/src/main/java/com/petlis/{common/security,controller/api,service/impl}/**`, `admin/**`, matching tests.
**Interfaces:** role assignments for DOCTOR/HOSPITAL_ADMIN/LAB_STAFF/COURIER/ADMIN; admin APIs return legacy `Result`.

- [ ] Write failing tests for role persistence, revoked-user tokens, resource scope and `code=200` admin responses.
- [ ] Implement `lin_user_role` mapping and resource authorization while preserving legacy JWT/login.
- [ ] Import `admin`, adapt API/pagination without changing mini-program behavior.
- [ ] Run backend and admin suites.

### Task 3: Shipment, Laboratory and Report Workflow

**Files:** `petlis-backend/src/main/java/com/petlis/{controller/api,service/impl,repository}/**`, `db/platform/**`, matching tests.
**Interfaces:** `/logistics`, `/lab/tasks`, `/report-workflows`; legacy order/detail IDs and status synchronization.

- [ ] Write failing tests for courier/lab scope, shipment states, result batches, retest, illegal report publish and payment/ownership.
- [ ] Implement shipment/task/result/report/template/clinical/idempotency services on legacy orders and new workflow tables.
- [ ] Generate Chinese PDF and publish to legacy detail/order; emit one legacy outbox event per transition.
- [ ] Run focused and all backend tests.

### Task 4: Contracts, Monthly Billing and Operational Completion

**Files:** contract/billing/admin services and controllers, existing coupon/gift/invoice/article/message services, `admin/**`, tests.
**Interfaces:** `/contracts`, `/bills`, `/admin/dashboard`, refund/print/export; existing mini-program contracts remain stable.

- [ ] Write failing tests for discounts/credit, monthly totals/reconciliation/settlement, refunds, cancellation compensation, dashboard aggregates and audits.
- [ ] Implement contract pricing, monthly bills, arrears/overdue, operational management, refund and print/export on unified masters.
- [ ] Complete admin pages against real APIs with role-appropriate actions.
- [ ] Run all backend/admin tests.

### Task 5: Mini-program Gap Completion Without UI Regression

**Files:** `miniprogram/config/**`, `miniprogram/utils/request.js`, `miniprogram/api/index.js`, existing health/order/coupon/mine pages, new bill/logistics views, tests.
**Interfaces:** preserve existing request signatures; real pet history, coupon detail, bills/reconciliation and shipment timeline.

- [ ] Add failing tests for authenticated refresh/logout, environment URL, health data, bill pages, coupon detail and logistics timeline.
- [ ] Implement minimal confirmed-design adaptations; do not replace pages/components or five tabs.
- [ ] Add page event/binding and 38-route completeness tests.
- [ ] Run mini-program and backend contract suites.

### Task 6: Full Cross-client Verification and PR

**Files:** `integration/**`, `README.md`, `docs/FUNCTION-MATRIX.md`, test evidence.
**Interfaces:** real HTTP backend on isolated MySQL; confirmed mini-program request modules and Playwright admin.

- [ ] Run the full cross-role scenario and negative cases; verify every Excel matrix row.
- [ ] Verify old schema snapshots unchanged and repeat migrations/tests; check browser screenshots and mini-program PDF.
- [ ] Run WeChat DevTools compilation if credentials permit; record external limits honestly.
- [ ] Update documentation, commit in Chinese, push and create a Chinese PR to `001-vet-lab-system`.
