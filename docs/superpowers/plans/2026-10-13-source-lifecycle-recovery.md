# Source Lifecycle Recovery Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let users safely restore inaccessible library sources and handle missing files without losing persisted source identity or user state.

**Architecture:** Extend the existing folder-picker result/action seam to carry a recovery target, validate platform handles before persistence, and serialize rebind through the App-owned library operation coordinator. Keep Android permission, desktop dropped-file, and iOS managed-folder recovery as their existing platform-specific flows. Reuse current scanner failure/completed semantics and explicit remove-missing.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Android SAF/MediaStore, JVM filesystem, iOS Foundation, OpenSpec.

**Spec:** `openspec/changes/source-lifecycle-recovery/specs/source-lifecycle-recovery/spec.md` and `openspec/changes/source-lifecycle-recovery/design.md`.

## Global Constraints

- Never delete source, track, favorite, history, metadata override, or playlist state automatically during recovery.
- Rebind must preserve the existing source ID and creation timestamp.
- Only a valid completed scan can authorize explicit remove-missing.
- Android MediaStore permission restoration does not automatically register or scan a source.
- iOS external Files.app content continues through the existing copy-to-managed-folder flow.
- No schema migration unless implementation proves existing source identity/handle fields cannot represent the behavior.

## Review Focus

- Failed root enumeration must never become a completed empty scan; prove it cannot authorize remove-missing.
- Android rebind cancellation/grant failure must preserve old grant and source row; a newly acquired unused grant is released safely.
- Rebind conflict must leave both source rows and track/user state unchanged.
- Same relative path in a replacement root may refer to different media; preserve established source-local identity behavior and do not claim content matching.
- iOS fixed managed-folder recovery and desktop dropped-file-set recovery must not expose arbitrary-folder rebind.

---

### Task 1: Explicit recovery outcome and source rebind contract

**Files:**
- Modify: `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibrarySource.kt`
- Modify: `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanModels.kt`
- Modify: `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- Test: `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryRepositoryContractTest.kt`
- Test: `feature/library/impl/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`

**Interfaces:**
- Produces a repository operation such as `rebindSource(sourceId: String, replacement: LibrarySource): SourceRebindResult` with explicit `Rebound`, `SourceNotFound`, `PlatformMismatch`, and `HandleConflict` outcomes. Final naming must follow existing result naming patterns.
- No new schema: verify source row upsert stores changed handle while retaining ID-owned relationships.

- [ ] Write failing repository tests proving successful rebind preserves the original ID, creation time, tracks, favorites, history and metadata overrides, and rejects another source’s handle without changing either row.
- [ ] Run the named Library repository tests and confirm the new behavioral assertions fail before production implementation.
- [ ] Implement atomic source rebind with same-platform eligibility and handle-conflict detection; do not mutate source kind or user-state rows.
- [ ] Run the focused contract and SQLDelight repository tests; verify pass and migration count remains unchanged.
- [ ] Commit `feat: add safe library source rebind contract`.

### Task 2: Platform-appropriate recovery selection

**Files:**
- Modify: `feature/library/api/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryRepository.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`
- Modify: `feature/library/impl/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.android.kt`
- Modify: `feature/library/impl/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.jvm.kt`
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformFolderPicker.kt`
- Modify/test: platform source-access and picker tests under `feature/library/impl/src/{androidHostTest,jvmTest,iosTest}`.

**Interfaces:**
- The folder selection action carries an optional existing source ID; ordinary add-source calls preserve current behavior.
- Recovery accepts only a readable folder whose platform kind matches the existing eligible folder source.

- [ ] Add tests for selecting a replacement source while preserving its target source ID, cancellation, invalid/unreadable root, and platform mismatch.
- [ ] Run the focused picker/access tests and confirm failures expose current inability to target an existing source.
- [ ] Implement the platform adapters: persist Android tree permission before reporting success; validate JVM directory; keep iOS managed root fixed and do not offer arbitrary rebind.
- [ ] Ensure a newly granted Android URI is released only if subsequent admission fails and it is not already owned by a source.
- [ ] Run Android host, JVM and iOS test-code compilation/adapter tests.
- [ ] Commit `feat: add platform source recovery selection`.

### Task 3: Coordinator-owned rebind and truthful scan state

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryOperationCoordinator.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Modify/test: `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt` and a focused rebind orchestration test.
- Modify/test: `feature/library/impl/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`.

**Interfaces:**
- App exposes one explicit rebind action that admits an operation, invokes the repository contract, and publishes the refreshed source/library state only for the current operation token.
- Rebind does not scan automatically; the user may explicitly request scan after successful rebind.

- [ ] Add failing tests for rebind rejected during admitted destructive work, stale publication after a newer operation, and source-root access failure producing `Failed` rather than `Completed`.
- [ ] Run focused Shared and scanner tests; confirm each assertion fails for the current behavior.
- [ ] Implement rebind admission/publication using the existing `LibraryOperationCoordinator`; preserve `CancellationException` behavior and do not add a parallel lock/state machine.
- [ ] Verify inaccessible-root scan failure leaves source tracks and authoritative completed scan summary untouched.
- [ ] Run focused Shared JVM and Library JVM tests.
- [ ] Commit `feat: coordinate non-destructive source recovery`.

### Task 4: Settings recovery UX by source kind

**Files:**
- Modify: `feature/library/impl/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- Modify: `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- Modify: Library/Settings EN and ZH Compose resources.
- Test: focused Library and Settings JVM route/UI tests and resource ownership tests.

**Interfaces:**
- Recovery row exposes source-neutral label/action and a callback; it does not import Android permission APIs or platform picker types.
- Android MediaStore recovery, Android SAF rebind, desktop dropped-file rescan, iOS managed-folder recreation, and ordinary source rescan each retain their distinct available action.

- [ ] Add UI tests for each source kind’s actionable recovery affordance, no destructive action on lost access, and accessible localized explanation.
- [ ] Run the focused tests to establish pre-implementation failure for missing rebind UX.
- [ ] Implement Settings recovery labels/actions and callback routing; cancellation and conflict produce recoverable feedback and no scan/removal.
- [ ] Test compact and wide source-management layouts, busy/inert state, EN/ZH semantics, and existing remove-missing report actions.
- [ ] Run focused Library and Settings JVM tests plus resource ownership checks.
- [ ] Commit `feat: add localized source recovery controls`.

### Task 5: Integrated validation and closeout

**Files:**
- Modify: `openspec/specs/local-library-scanning/spec.md`
- Modify: `openspec/changes/source-lifecycle-recovery/tasks.md`
- Modify: `roadmap.md`
- Modify: `progress.md`

- [ ] Run Library repository/scanner, Shared coordinator, Settings UI, Android host, iOS test-code compilation and desktop compilation gates.
- [ ] Run `spotlessApply`, `spotlessCheck`, `detekt`, `architectureCheck`, strict OpenSpec validation, and `git diff --check` as separate gates.
- [ ] Verify no database migration was introduced unless the repository contract requires it; if schema changes exist, run generated SQLDelight migration/database tests.
- [ ] Obtain independent full-change review; resolve Critical/Important findings and rerun affected regressions.
- [ ] Sync the canonical local-library-scanning spec, archive the change, record exact automated results and user-owned manual acceptance in progress/roadmap, and commit.
