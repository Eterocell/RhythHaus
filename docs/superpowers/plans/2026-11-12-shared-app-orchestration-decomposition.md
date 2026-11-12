# Shared App Orchestration Decomposition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: use `superpowers:executing-plans` to execute these sequential, shared-state tasks. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce the responsibility concentration in Shared `App.kt` while preserving every existing boundary and observable behavior.

**Architecture:** Keep `:shared` as the sole composition root. Move app-owned publication, operation, playlist-document, theme, and onboarding host logic into focused internal Shared files, then make `App()` an explicit dependency/composition handoff. Reuse `AppProcessState`, `AppLibraryOrchestrator`, `AppLibraryOperationCoordinator`, `LibraryAppState`, `PlaylistStateOwner`, and preference owners; do not add a second owner for any of their state or policy.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Gradle, existing Koin and coroutine wiring.

**Spec:** [OpenSpec proposal](../../../openspec/changes/shared-app-orchestration-decomposition/proposal.md), [OpenSpec design](../../../openspec/changes/shared-app-orchestration-decomposition/design.md), and [approved Superpowers design](../specs/2026-11-12-shared-app-orchestration-decomposition-design.md).

## Global Constraints

- Preserve the `App(...)` callable signature, Kotlin package names, feature APIs, dependency graph, resource ownership, database schema, iOS exports, and platform seams.
- Preserve route/Back policy, scan admission and cancellation, source recovery, desktop drop admission, publication revisions, playlist document revision guards, persisted preference behavior, and all user-visible copy/semantics.
- Do not introduce a new Gradle module, dependency, service locator, generic state/event framework, compatibility shim, or parallel coordinator.
- Keep Compose state/effects at the same logical owner and preserve `remember` identity, `LaunchedEffect` keys, coroutine cancellation identity, and non-cancellable cleanup.
- Do not expand the Shared inventory allow-list until the baseline `ThinSharedInventoryTest` result is known and any failure has a diagnosed cause.
- Device/audio acceptance is not implied by refactor tests or builds. Any desktop smoke uses a random empty `user.home` and synthetic-only media.

## Review Focus

1. Newer authoritative library publications must not be overwritten by stale revisions; prove with existing publication/revision regressions.
2. Scan/import cancellation must remain the same `CancellationException`, release the follow-up gate, and preserve admitted-operation serialization.
3. Source removal, remove-missing, clear, and playback reconciliation must keep their existing failure, cancellation, and state-publication behavior.
4. Playlist export/import must retain stale-revision rejection, exactly-once publication, retry/cancel, and document-launch callback behavior.
5. Theme/onboarding persistence must remain non-optimistic and retryable; their host extraction must not reset unrelated library or playback state.

---

## File structure

**Modify:**
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` — retain `App(...)` as the entry and dependency handoff.
- `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ThinSharedInventoryTest.kt` — update the exact approved inventory only after the final Shared source set is known.
- `docs/architecture.md` and `docs/adr/0001-feature-first-module-boundaries.md` — document the final Shared file ownership without changing module boundaries.
- `progress.md` and `roadmap.md` — record exact verification and the completed refactor only after acceptance.

**Create:**
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryPublication.kt` — library content projections, authoritative publication owner, and publication/history adapters moved unchanged from `App.kt`.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryOperations.kt` — scan/source-operation terminal helpers and mutation/reconciliation adapters moved unchanged from `App.kt`.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppPlaylistBackupHost.kt` — Shared playlist document/controller host and revision-guard adapter.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryContentHost.kt` — app-local library state/effect composition and `LibraryHomeScreen` binding; contains only Shared orchestration, not feature implementations.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppOnboardingGate.kt` — Shared onboarding preference gate/loading presentation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/RhythHausTheme.kt` — the existing single Shared Haus/Miuix/Material3 theme boundary and scheme mappings.

No public declaration or API file is added. If implementation review shows a candidate file would only relocate a large block without creating a cohesive owner, keep it with its existing owner and amend the plan before proceeding.

## Internal composition interfaces

`App()` retains its current external parameters and Koin construction. It explicitly builds and passes these internal groups; child composables do not call Koin:

```kotlin
internal data class AppLibraryServices(
    val repository: LibraryRepository,
    val playlistRepository: PlaylistRepository,
    val platformAccess: PlatformSourceAccess,
    val scanner: LibraryScanner,
    val tagLibReader: TagLibReader,
    val processState: AppProcessState,
    val playbackReconciler: PlaybackSessionReconciler,
    val playlistStateOwner: PlaylistStateOwner,
)

internal data class AppPlaybackServices(
    val controller: PlaybackController,
    val outputStatusProvider: PlaybackOutputStatusProvider?,
    val replayGainPreferenceOwner: ReplayGainPreferenceOwner,
    val crossfadePreferenceOwner: CrossfadePreferenceOwner,
    val gaplessPreferenceOwner: GaplessPreferenceOwner,
    val automaticResumePreferenceOwner: AutomaticResumePreferenceOwner,
)

internal data class AppPreferenceServices(
    val themePreferenceStore: ThemePreferenceStore,
    val themePreferenceOwner: ThemePreferenceOwner,
    val onboardingPreferenceStore: OnboardingPreferenceStore,
)

internal data class AppPlatformServices(
    val notificationPermissionController: MediaNotificationPermissionController,
    val mediaStorePermissionController: MediaStoreAudioPermissionController,
    val desktopDropEvents: Flow<List<String>>?,
    val desktopDropActive: Boolean,
    val onDesktopDropAvailabilityChanged: (Boolean) -> Unit,
)

@Composable
internal fun AppLibraryContentHost(
    library: AppLibraryServices,
    playback: AppPlaybackServices,
    preferences: AppPreferenceServices,
    platform: AppPlatformServices,
)
```

These types are internal implementation handoffs, not a new DI container or feature API. Existing `LibraryHomeScreen` inputs and callback meanings remain unchanged.

## Task 1: Establish the baseline and test boundaries

**Files:** no production files.

- [ ] **Step 1: Run the thin Shared inventory test before editing.**

Run: `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ThinSharedInventoryTest' --configuration-cache`

Expected: the exact current inventory assertion passes. If it fails, stop before code edits and record the exact extra/missing paths; do not widen the allow-list as a shortcut.

- [ ] **Step 2: Record the affected Shared behavior baseline.**

Run:

```bash
./gradlew :shared:jvmTest \
  --tests '*AppScanCancellationTest*' \
  --tests '*AppMutationCancellationTest*' \
  --tests '*LibrarySourceManagementTest*' \
  --tests '*AppLibraryImportTest*' \
  --tests '*AppOnboardingLifecycleJvmTest*' \
  --tests '*PlaylistBackupRevisionGuardAdapterTest*' \
  --configuration-cache
```

Expected: all selected tests pass. A failure blocks extraction until classified under the repository's systematic-debugging workflow.

## Task 2: Extract authoritative library publication

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryPublication.kt`
- Test: existing `AppScanCancellationTest`, `AppMetadataOverrideTest`, `PlaybackHistoryAdmissionTest`, `LibrarySourceManagementTest`, and `AppMutationCancellationTest`.

- [ ] **Step 1: Move the current publication declarations without changing signatures.** Move `LibraryContentState`, `AppLibraryContentState`, `AuthoritativeLibraryPublication`, `AuthoritativeLibraryPublicationOwner`, `AuthoritativeRevisionResult`, `InitialLibraryPublicationState`, `ScanPublicationState`, and `LibraryMutationPublication` to `AppLibraryPublication.kt`.
- [ ] **Step 2: Move publication-owned helpers.** Move `publishInitialLibraryContent`, `publishScanContentAfterReconcile`, `loadLibraryContent`, `recordPlaybackHistoryAndPublish`, `collectPlaybackHistoryAndPublish`, and `setTrackFavoriteAndPublish`; retain package and internal visibility.
- [ ] **Step 3: Run the task-owned regressions.** Run the Task 1 focused Shared command. Expected: unchanged assertions pass, including stale-revision ordering, favorite/history publication, reconciliation failure/cancellation, and original cancellation propagation.

## Task 3: Extract scan, source, mutation, and picker operations

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryOperations.kt`
- Test: existing `AppScanCancellationTest`, `AppMutationCancellationTest`, `LibrarySourceManagementTest`, `AppLibraryImportTest`, `AppLibraryOrchestratorTest`, and `LibraryOperationCoordinatorTest`.

- [ ] **Step 1: Move existing operation result and publication helpers.** Move the source remove/remove-missing/clear functions, `publishLibraryContentAfterReconcileFailureSafe`, rejection/result projections, terminal scan restoration/cancellation helpers, picker and desktop-drop terminal resolution, mutation gate, and follow-up scan settlement helpers to `AppLibraryOperations.kt` without changing their inputs, outputs, or cancellation behavior.
- [ ] **Step 2: Keep operation authority unchanged.** `AppLibraryOrchestrator` and `AppLibraryOperationCoordinator` remain the only admission/token owners. Do not copy token, mutex, scan-job, or pending-gate state into the new file.
- [ ] **Step 3: Run the operation regressions.** Run the Task 1 focused Shared command plus `./gradlew :shared:jvmTest --tests '*AppLibraryOrchestratorTest*' --tests '*LibraryOperationCoordinatorTest*' --configuration-cache`. Expected: cancellation, admission rejection, source rebind/removal, missing-track authority, desktop import normalization, and pending-gate release remain unchanged.

## Task 4: Extract playlist-document, theme, and onboarding hosts

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppPlaylistBackupHost.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppOnboardingGate.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/RhythHausTheme.kt`
- Test: existing playlist backup, onboarding lifecycle, theme mapping, and system-bar appearance tests.

- [ ] **Step 1: Move playlist document hosting.** Move `runPlaylistBackupOperation` and `authoritativePlaylistBackupRevisionGuard`, then isolate the document launcher/controller/state callback group in `AppPlaylistBackupHost.kt`. Preserve the existing `LibraryHomeScreen` inputs `playlistBackupState`, `backupDocumentAvailable`, `onExportPlaylists`, `onOpenPlaylistBackup`, `onExportPlaylistFormat(playlistId, format)`, `onOpenPlaylistFormat(format)`, `onConfirmPlaylistBackup`, and `onPlaylistBackupAction`; preserve feature controller ownership and exact cancellation/stale-revision behavior.
- [ ] **Step 2: Move the onboarding host gate.** Move `AppOnboardingPresentation`, `AppOnboardingGate`, and `OnboardingLoadingSurface` to `AppOnboardingGate.kt`; keep preference persistence and screen behavior unchanged.
- [ ] **Step 3: Move the single theme boundary.** Move `resolveSharedHausTheme`, `RhythHausTheme`, `hausMiuixColorScheme`, and `hausMaterial3ColorScheme` to `theme/RhythHausTheme.kt`; preserve the one `RhythHausTheme` owner and `AppSystemBarAppearance` placement.
- [ ] **Step 4: Run regression tests.** Run `./gradlew :shared:jvmTest --tests '*PlaylistBackupRevisionGuardAdapterTest*' --tests '*SettingsPlaylistBackupEmbeddingTest*' --tests '*AppOnboardingLifecycleJvmTest*' --tests '*ThemeColorSchemeMappingJvmTest*' --tests '*AppSystemBarAppearanceJvmTest*' --configuration-cache`. Expected: tests prove existing behavior, not helper forwarding.

## Task 5: Reduce the root composable to composition and handoff

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- Create: `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AppLibraryContentHost.kt`
- Test: existing Shared route, shell, onboarding, and settings integration tests.

- [ ] **Step 1: Add the internal service groups defined above.** Construct `AppLibraryServices`, `AppPlaybackServices`, `AppPreferenceServices`, and `AppPlatformServices` explicitly in `App()` from its current `koinInject` results and parameters; no child composable may call Koin or obtain a hidden service locator.
- [ ] **Step 2: Extract the app-local library state and `LibraryHomeScreen` binding.** Move the current library states, scan/picker/drop callbacks, and screen parameter assembly into `AppLibraryContentHost(library, playback, preferences, platform)`. The host delegates to the extracted publication/operation/document owners and `LibraryAppState`; do not add a parallel `LibraryAppState`, operation coordinator, or publication owner.
- [ ] **Step 3: Keep the `App(...)` signature and app tree unchanged.** `App()` remains the Shared entry, passes the same optional permission/drop inputs, applies the same single theme/onboarding gates, and composes the same `LibraryHomeScreen` state/actions. Any internal typed input groups must represent cohesive owners rather than one undifferentiated service bag.
- [ ] **Step 4: Run production-boundary integration tests.** Run `./gradlew :shared:jvmTest --tests '*LibraryAppShellJvmTest*' --tests '*LibraryRouteAdapterJvmTest*' --tests '*SettingsRouteAdapterJvmTest*' --tests '*AppOnboardingLifecycleJvmTest*' --configuration-cache`. Expected: route/back ownership, screen state, callback policy, and settled UI projection remain unchanged.

## Task 6: Reconcile architecture inventory and documentation

**Files:**
- Modify: `build-logic/convention/src/test/kotlin/com/eterocell/gradle/architecture/ThinSharedInventoryTest.kt`
- Modify: `docs/architecture.md`
- Modify: `docs/adr/0001-feature-first-module-boundaries.md`
- Modify: `progress.md` and `roadmap.md`.

- [ ] **Step 1: Update the exact inventory only after ownership review.** Add or remove only the concrete source paths proved to be Shared facade/composition ownership; the test must still reject a new Library/Playlist implementation file in Shared.
- [ ] **Step 2: Update architecture records.** Describe the final file responsibilities while retaining Shared's sole composition root, Back arbitration, lifecycle, Koin assembly, and iOS facade. No package or Gradle graph changes.
- [ ] **Step 3: Run the inventory and architecture checks.** Run the exact Task 1 inventory command and `./gradlew architectureCheck --configuration-cache`. Expected: exact file set and dependency graph pass.

## Task 7: Integrated verification and handoff

**Files:** all production/test/docs files above.

- [ ] **Step 1: Run focused quality gates.** Run `./gradlew qualityCheck --configuration-cache` and the focused Shared behavior commands from Tasks 2–5.
- [ ] **Step 2: Run first-platform verification.** Run `./init.sh`; record exact command and output. Do not claim a platform passed if the script exits before that platform's task.
- [ ] **Step 3: Run strict OpenSpec and diff hygiene.** Run `openspec validate shared-app-orchestration-decomposition --strict` and `git diff --check`.
- [ ] **Step 4: Run isolated desktop smoke.** Launch the app with a fresh random `user.home` asserted before process startup and synthetic-only fixture data. Capture settled screenshot and accessibility hierarchy for Library plus one navigation/player transition; verify state is stable, not merely that input was sent. Do not import, inspect, or play personal media. This smoke is not audible/device acceptance.
- [ ] **Step 5: Review and commit.** Review the complete diff against this design and the OpenSpec task set, then create one conventional commit: `refactor(shared): split app orchestration`.

## Execution handoff

Use `executing-plans` for the implementation because the tasks share state and must land sequentially. Keep the OpenSpec change open until all tasks, verification, review, and required handoff are complete; archive only through the OpenSpec archive workflow after acceptance.
