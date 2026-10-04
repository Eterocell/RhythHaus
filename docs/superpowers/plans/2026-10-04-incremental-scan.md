# Incremental Scan Experience Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Persist and display a bounded, localized summary of changes from each successful explicit library scan.

**Architecture:** Extend the existing scan session/report persistence rather than create a parallel scan path. LibraryScanner snapshots source-scoped tracks before scanning, classifies completed observations, and writes one summary; Shared publishes it through existing scan report state; Settings renders it and leaves removal explicit.

**Tech Stack:** Kotlin Multiplatform, SQLDelight, Compose Multiplatform, existing LibraryOperationCoordinator and Settings scan-report surfaces.

**Spec:** `openspec/changes/incremental-scan/specs/local-library-scanning/spec.md`

## Global Constraints
- Explicit scans only; no startup scan or background watcher.
- Missing reporting never deletes data; Remove missing remains explicit.
- Failed/cancelled scans do not replace the latest authoritative summary.
- At most 100 affected details per category.
- Shared owns orchestration; feature/library owns scan and persistence contracts.
- Every SQLDelight schema change includes a migration.

## Review Focus
- Same source-local key with changed size/time retains the same track ID: repository/scanner regression.
- Missing row remains playable and retains favorite/history/override: remove-missing regression.
- Cancelled or failed scan cannot publish a partial summary: scanner/coordinator regression.
- More than 100 affected rows remains bounded: persistence regression.
- Compact Settings can reach summary and distinguish report from deletion: Compose semantics regression.

---

### Task 1: Summary model and persistence
**Files:** Create `feature/library/api/.../ScanChangeSummary.kt`; modify scan session schema, migration, generated repository mapping; test database and repository contracts.

- [ ] Write failing migration and round-trip tests for four counts, bounded details, and nullable summary on old rows.
- [ ] Run `:core:database:jvmTest` and repository JVM test; expect RED before schema/API exists.
- [ ] Add versioned SQLDelight summary columns/relationship and migration; expose immutable API model with category counts and bounded details.
- [ ] Run database and repository tests; expect PASS and verify schema generation.
- [ ] Commit `feat: persist scan change summaries`.

### Task 2: Scanner classification
**Files:** Modify `feature/library/impl/.../LibraryScanner.kt`; add scanner tests.

- [ ] Write RED tests for unchanged, added, modified, missing, cancellation/failure, and >100 detail bounds.
- [ ] Snapshot source-scoped tracks by stable local key before scan; classify only after successful completion using known size/modification fields.
- [ ] Persist one summary while preserving existing track IDs and leaving missing rows untouched.
- [ ] Run focused scanner/repository tests; expect PASS.
- [ ] Commit `feat: classify library scan changes`.

### Task 3: Shared publication
**Files:** Modify Shared scan report model/orchestration and tests.

- [ ] Write RED tests proving completed summaries publish and failed/cancelled scans retain the previous summary.
- [ ] Thread summary through the existing App-owned scan progress/terminal report without a second coordinator.
- [ ] Run focused Shared scan cancellation/report tests; expect PASS.
- [ ] Commit `feat: publish scan change summaries`.

### Task 4: Settings UI and localization
**Files:** Modify Settings scan report composables/resources and JVM semantics tests.

- [ ] Write RED semantics tests for EN/ZH labels, four counts, bounded detail display, explicit missing/remove distinction, and compact reachability.
- [ ] Render summary only for latest completed scans; retain prior summary for failed/cancelled reports; add accessible headings and live-region wording.
- [ ] Run Settings JVM tests and resource ownership checks; expect PASS.
- [ ] Commit `feat: show localized scan change summaries`.

### Task 5: Review, gates, and closeout
- [ ] Run feature/library, database, Shared, Settings, desktop, Android-host where available, iOS test compilation, Spotless, Detekt, Architecture Check, and strict OpenSpec validation.
- [ ] Obtain independent review focused on identity preservation, cancellation, bounded persistence, and deletion safety; fix and re-review any findings.
- [ ] Sync canonical `local-library-scanning`, archive the change, update `roadmap.md` and `progress.md` with exact evidence and manual acceptance limits.
- [ ] Commit closeout with a conventional message.
