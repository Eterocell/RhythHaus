# Tasks

## 1. Establish baselines

- [x] 1.1 Run `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ThinSharedInventoryTest' --configuration-cache` and record the current exact Shared inventory result before any code edit; stop if it fails and do not widen the allow-list without root-cause evidence.
- [x] 1.2 Run the focused Shared baseline for `AppScanCancellationTest`, `AppMutationCancellationTest`, `LibrarySourceManagementTest`, `AppLibraryImportTest`, `AppOnboardingLifecycleJvmTest`, and `PlaylistBackupRevisionGuardAdapterTest` with `:shared:jvmTest`; record the pre-refactor result.

Evidence (2026-11-12, isolated worktree, before production edits): `./gradlew :shared:jvmTest --tests '*AppScanCancellationTest*' --tests '*AppMutationCancellationTest*' --tests '*LibrarySourceManagementTest*' --tests '*AppLibraryImportTest*' --tests '*AppOnboardingLifecycleJvmTest*' --tests '*PlaylistBackupRevisionGuardAdapterTest*' --configuration-cache` → BUILD SUCCESSFUL; 111 tests, 0 failures, 0 errors, 0 skipped. Task 1.1 remains open: its exact command failed on 61 pre-existing files outside the inventory, before any refactor. No inventory change or extraction has been made.

Recovery evidence: user authorized ownership audit and reconciliation; later archived app-facade/preference/shell decisions justify all 61 retained files (see `docs/architecture.md`, retained additions table). The literal inventory was updated without changing either exact-set assertion. A throwaway unapproved file caused the exact test to fail naming only `UnapprovedInventoryProbe.kt`; after deleting the control, the exact inventory command and six-class Shared baseline both passed through `task-done` (2026-11-12). Earlier failure above is historical evidence; the baseline gate is now cleared.

## 2. Extract authoritative library publication

- [x] 2.1 Move the existing library content/publication models and revision owner plus initial-publication, scan-publication, history, and favorite publication helpers into `AppLibraryPublication.kt` without changing package, visibility, or signatures; verify the focused publication/history/cancellation tests pass.

Evidence: Task 2 `task-done` ran `:shared:jvmTest` with `--tests '*AppScanCancellationTest*' --tests '*AppMutationCancellationTest*' --tests '*LibrarySourceManagementTest*' --tests '*AppLibraryImportTest*' --tests '*AppOnboardingLifecycleJvmTest*' --tests '*PlaylistBackupRevisionGuardAdapterTest*' --tests '*AppMetadataOverrideTest*' --tests '*PlaybackHistoryAdmissionTest*' --configuration-cache`: BUILD SUCCESSFUL. The private scan-terminal cancellation projection moved with its sole publication consumer; state declarations, signatures, and logic are unchanged.

## 3. Extract scan and source-operation adapters

- [x] 3.1 Move existing source remove, remove-missing, clear, reconciliation-failure, picker/drop terminal, scan-restoration, cancellation, and mutation-gate helpers into `AppLibraryOperations.kt`; retain `AppLibraryOrchestrator` and `AppLibraryOperationCoordinator` as the sole token/admission owners; verify import, source-management, mutation, scan-cancellation, and coordinator tests pass.

Evidence: Task 3 `task-done` ran `:shared:jvmTest` with the six Task 1 filters plus `--tests '*AppLibraryOrchestratorTest*' --tests '*LibraryOperationCoordinatorTest*' --configuration-cache`: BUILD SUCCESSFUL. Moved helpers retain signatures, cancellation and admission behavior; the existing orchestrator/coordinator are unchanged.

## 4. Extract playlist, theme, and onboarding hosts

- [x] 4.1 Move playlist document hosting/revision-guard adaptation into `AppPlaylistBackupHost.kt`, the onboarding preference gate into `AppOnboardingGate.kt`, and the single Shared theme wrapper/scheme mapping into `theme/RhythHausTheme.kt`; verify playlist backup, onboarding lifecycle, theme mapping, and system-bar appearance JVM tests pass.

Evidence: Task 4 `task-done` ran `:shared:jvmTest --tests '*PlaylistBackupRevisionGuardAdapterTest*' --tests '*SettingsPlaylistBackupEmbeddingTest*' --tests '*AppOnboardingLifecycleJvmTest*' --tests '*ThemeColorSchemeMappingJvmTest*' --tests '*AppSystemBarAppearanceJvmTest*' --configuration-cache`: BUILD SUCCESSFUL. Theme functions retain their Kotlin package; onboarding stays at its original composition lifetime. Playlist document host receives the existing coroutine scope and latest authoritative library/revision/playlist projections through `rememberUpdatedState`; feature controller/reducers remain unchanged.

## 5. Reduce the root composition body

- [x] 5.1 Add the plan-defined `AppLibraryServices`, `AppPlaybackServices`, `AppPreferenceServices`, and `AppPlatformServices`, constructed explicitly in `App()`; add `AppLibraryContentHost(library, playback, preferences, platform)` with no Koin/service-locator access, and verify the existing Shared route/shell/settings/onboarding adapter tests pass.
- [x] 5.2 Reduce `App()` to dependency construction and composition handoff while preserving its external signature, the existing theme/onboarding tree, `LibraryHomeScreen` inputs, callback behavior, and remembered-state lifetimes; verify Shared integration tests pass and no second owner is introduced.

Evidence: Task 5 `task-done` ran `:shared:jvmTest --tests '*LibraryAppShellJvmTest*' --tests '*LibraryRouteAdapterJvmTest*' --tests '*SettingsRouteAdapterJvmTest*' --tests '*AppOnboardingLifecycleJvmTest*' --tests '*AppScanCancellationTest*' --tests '*AppMutationCancellationTest*' --tests '*AppLibraryImportTest*' --configuration-cache`: BUILD SUCCESSFUL. Root is explicit DI/composition; the additional internal `AppLibraryOperationsHost` separates workflows rather than relocating a giant composable unchanged. Existing admission/publication/process/Back owners remain in use.

## 6. Reconcile the Shared inventory and architecture records

- [x] 6.1 Update `ThinSharedInventoryTest` with only the final Shared-owned composition files and update `docs/architecture.md` plus ADR 0001 to match the final ownership; verify the exact inventory test and `./gradlew architectureCheck --configuration-cache` pass.

Evidence: Task 6 `task-done` ran `./gradlew :build-logic:convention:test --tests 'com.eterocell.gradle.architecture.ThinSharedInventoryTest' --configuration-cache && ./gradlew architectureCheck --configuration-cache`: both BUILD SUCCESSFUL. Inventory remains literal and both extras/missing assertions are unchanged; the Task 1 negative control proved unapproved source rejection.

## 7. Integrated verification and handoff

- [x] 7.1 Run `./gradlew :shared:jvmTest --configuration-cache`, `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, `/usr/bin/xcrun xcodebuild -version`, `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`, `./gradlew qualityCheck --configuration-cache`, and the exact Shared inventory test; record each exact result.
- [x] 7.2 Run `./init.sh`, `openspec validate shared-app-orchestration-decomposition --strict`, and `git diff --check`; record any exact blocker and do not infer a platform pass from tasks that did not execute.
- [ ] 7.3 Launch desktop with an asserted fresh random `user.home` and synthetic-only media; capture settled screenshot and accessibility hierarchy for Library and one route/player transition, then stop the process. Do not inspect or play personal media; this is not audible/device acceptance.
- [x] 7.4 Review the complete diff against the approved design and plan, update progress/roadmap evidence, and commit the completed OpenSpec workflow with `refactor(shared): split app orchestration`.

Integrated evidence (2026-11-12): `./init.sh` completed all three stages successfully: combined `:shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`, Xcode version, and `:shared:iosSimulatorArm64Test --configuration-cache`. Shared XML reports contain 567 JVM tests and 339 iOS tests, all zero failures/errors/skips. `qualityCheck` initially found pre-existing formatting in `androidApp/build.gradle.kts` and `taglib/build.gradle.kts`; applying only those requested whitespace changes cleared `./gradlew qualityCheck --configuration-cache`. Strict named OpenSpec validation and `git diff --check` passed in the same successful quality command. Exact inventory/architecture checks passed under Task 6.

Desktop smoke limitation: the app launched with explicit asserted `HOME` and JVM `user.home` at a fresh `/tmp/rhythhaus-app-split-*` directory. Orca discovered the owned 800×600 RhythHaus window (PID 8854, window 6428), but both PID-targeted and exact-window AX reads failed with `permission_denied` (visible window but no accessibility window after 1500ms). Orca reports Accessibility and Screenshots granted; no permissions were changed and no fallback coordinate interaction or personal media was used. The isolated process was stopped. Task 7.3 remains unchecked: launch evidence is not screenshot/AX/interaction acceptance. The user explicitly deferred this unavailable smoke and authorized committing after final review, keeping the change unarchived (2026-11-12).

Final review: `ReviewAppDecomposition` compared extracted declaration/workflow bodies against original HEAD App.kt and reported no P0–P2 findings. Two cosmetic/dead-code minors remain deferred: unused Compose delegate imports in App.kt and the already-unused private terminalAfterFailure helper. Visibility of the cross-file onboarding loading helper and separate UI/invocation-time mutation-gate observations are intentional. No external API or behavior change was found.

## Workflow follow-up

- After all implementation tasks, verification, and review are complete, archive this OpenSpec change through the repository's OpenSpec archive workflow. Do not archive before completion.
