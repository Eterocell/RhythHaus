## 1. Page-Scoped Selection State

- [x] 1.1 Add a pure route/page-keyed track-selection reducer with start, select, toggle, visible-result reconciliation, cancellation, completion, and ordered-visible-ID behavior.
- [x] 1.2 Add RED/GREEN common tests for final-deselection exit, route cleanup, Search hidden-ID cleanup, idempotent long-press selection, and visible-order submission.

## 2. Eligible Track Rows and Accessibility

- [x] 2.1 Add one combined click/long-click contract and adapt Library home Songs, album detail, artist detail, and Search without changing ordinary playback clicks.
- [x] 2.2 Remove eligible per-row Add to Playlist buttons, render checkboxes before artwork only in selection mode, and keep now-playing and selected state independent.
- [x] 2.3 Add JVM Compose semantics and dispatch tests for non-gesture selection entry, checked/toggle state, exactly-once activation, and no playback on long press.

## 3. Contextual Bar and Generalized Picker

- [x] 3.1 Add one reusable selection bar and route-aware bottom-slot policy that makes it mutually exclusive with Now Playing and preserves measured safe-area/list clearance.
- [x] 3.2 Generalize picker and inline-create state from one track ID to an ordered non-empty list while preserving the playlist-detail browser.
- [x] 3.3 Wire Back, navigation, browse-mode changes, picker dismissal, mutation failure, and successful completion to the approved selection lifetime.
- [x] 3.4 Add RED/GREEN state, navigation, bottom-mode, and semantics tests for ordering, retention, cleanup, and mutual exclusion.

## 4. Strict Backup Codec and Matching

- [x] 4.1 Add version-1 backup models, canonical compact UTF-8 JSON writer, strict fixed-schema parser, and common CRC32 implementation without a new JSON dependency.
- [x] 4.2 Enforce the exact format discriminator, key order, checksum representation, 4 MiB/count/string/duration bounds, duplicate/unknown/missing-key rejection, integer-only numbers, malformed UTF-8 rejection, and trailing-content rejection.
- [x] 4.3 Add portable text normalization, inclusive ±2-second unique matching, deterministic conflict naming, preview counts, issue records, stale library revision, and all-unmatched playlist skipping.
- [x] 4.4 Add RED/GREEN golden, round-trip, malformed-input, boundary, normalization, unique/unmatched/ambiguous, duplicate-occurrence, export-validation, and import-plan tests.

## 5. Transactional Repository Import

- [x] 5.1 Add a multi-playlist repository import contract that validates every request and creates all eligible playlists and ordered duplicate entries in one transaction.
- [x] 5.2 Implement in-memory staging and one SQLDelight transaction without changing the persisted schema.
- [x] 5.3 Add common contract and real JVM SQLDelight RED/GREEN tests proving request order, duplicate preservation, rollback after a second-playlist failure, and pre-existing-state retention.

## 6. Platform Document Integration

- [x] 6.1 Add common save/open outcomes and a Compose platform-launcher seam with bounded byte ownership and silent cancellation.
- [x] 6.2 Implement Android create/open document contracts without broad storage permission and test exact-limit, oversized, cancellation, and I/O result mapping.
- [x] 6.3 Implement JVM/macOS native save/load dialogs without changing folder-picker behavior and test injected path/I/O seams without interactive dialogs.
- [x] 6.4 Implement and compile the iOS document-picker bridge with retained presentation ownership, balanced security-scoped access, temporary-export cleanup, bounded reads, and bridge tests.

## 7. Settings Backup Workflow

- [x] 7.1 Add pure Settings backup UI state for export, open, preview, result, recoverable failures, and dismissal.
- [x] 7.2 Build and validate export bytes before opening save; decode and plan imports before writes; reject stale previews against the authoritative library revision.
- [x] 7.3 Add Settings export/import actions, busy states, scrollable preview and result dialogs, accessible issue rows, and complete English/Chinese resources.
- [x] 7.4 Confirm import through exactly one repository mutation, retain preview and confirmed state on failure, refresh once on success, and rethrow coroutine cancellation.
- [x] 7.5 Add RED/GREEN workflow and JVM semantics tests for cancellation, distinct errors, preview counts, no-op disablement, stale rejection, retry retention, and result reporting.

## 8. Integration and Platform Verification

- [x] 8.1 Add a real JVM database integration test spanning visible-order selection, generalized picker behavior, export/decode/match, stale rejection, rollback, duplicate restore, conflict naming, and repeated import.
- [x] 8.2 Run focused selection, picker, codec, matching, repository, platform-adapter, Settings, and integration test groups with zero skipped required tests.
- [x] 8.3 Run strict OpenSpec validation and the supported JVM, Android host/debug, desktop compile, Xcode, and iOS simulator gates; add a SQLDelight migration only if the final diff unexpectedly changes persisted schema.

## 9. Runtime QA, Review, and Evidence

- [ ] 9.1 Exercise supported-page long press/tap, checkbox and accessibility toggles, Back/navigation/Search reconciliation, contextual-bar clearance, picker dismissal/failure/success, and now-playing coexistence on available runtimes.
- [ ] 9.2 Exercise Android, iOS, and JVM system panels plus valid, cancelled, oversized, malformed, stale, conflicting, unmatched, ambiguous, repeated, and failed import paths where targets are available.
- [ ] 9.3 Run visual QA for compact/wide, light/dark, English/Chinese, checkbox and row fit, focus order, bottom-bar mutual exclusion, and long preview reports; record unavailable target states as unverified.
- [x] 9.4 Complete task-scoped and final code review with no open Critical or Important finding, update `progress.md`, `roadmap.md`, and task reports with exact evidence, and leave archival for an explicit later request.
