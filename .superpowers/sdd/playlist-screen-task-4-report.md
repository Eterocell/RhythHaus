# Playlist Screen Task 4 Report

## Status

DONE_WITH_CONCERNS

OpenSpec `playlist-screen` tasks 4.1-4.4 are complete and committed. Task 5-7 statuses are unchanged. The focused Task 4 tests, JVM/desktop/Android verification, strict OpenSpec validation, resource parity, diff hygiene, and targeted post-fix review passed. The iOS simulator task remains blocked by unchanged JVM-only `Thread` references in `AppScanCancellationTest.kt`; no iOS test pass is claimed.

## Scope delivered

- Added exhaustive typed `LibraryRoute.PlaylistHub` and `LibraryRoute.PlaylistDetail(playlistId)` routes.
- Preserved existing Now Playing eligibility, settings-overlay policy, compact navigation, and wide detail replacement behavior.
- Added a pure `PlaylistState` reducer with selected tab, confirmed snapshot, loading, retryable read failure, mutation/recoverable notices, picker state, and browser state scaffolding.
- Added exact visible-order saved-playlist mapping from `PlaylistEntry.id` to `QueueOccurrence.id`, including duplicate tracks and occurrence row keys.
- Injected `PlaylistRepository` in `App`, which owns playlist state and confirmed refresh publication.
- Added `PlaylistStateOwner`, which serializes playlist reads/writes with one mutex and executes repository work off the UI dispatcher.
- Refreshed playlist snapshots after confirmed playlist writes and after rescan, source removal, and clear-library reconciliation/publication paths.
- Threaded repository, state, actions, retry, and mutation callbacks through `LibraryHomeScreen`, `LibraryRouteContent`, and `LibraryRouteOverlays`.
- Added the minimal Library home Playlists entry and loading/read-error/retry/recoverable route scaffold using existing Miuix, HausColors, safe chrome, and accessibility conventions.
- Added matching English and Chinese strings for approved Task 5/6 actions, confirmations, queue outcomes, and accessibility/state descriptions without implementing those workflows.
- Did not add full Saved/Queue screens, CRUD dialogs, drag UI, row overflow picker, searchable browser UI, or queue controls.

## Strict RED/GREEN evidence

### Initial route/state/lifecycle RED

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --configuration-cache
```

Result: expected compile failure for missing `PlaylistHub`, `PlaylistDetail`, playlist reducer/model/mapping APIs, and lifecycle parameters. One test fixture omitted `PlayableTrack.durationMillis`; that test-only construction error was corrected before implementation.

### Focused route/state GREEN

Same command after implementation: PASS (`BUILD SUCCESSFUL in 9s`, 26 tasks).

### Lifecycle RED/GREEN

- Source removal and clear-library tests initially failed to compile because `playlistRepository`/`updatePlaylists` seams did not exist; after implementation, `LibrarySourceManagementTest` passed.
- Rescan/stale-recovery tests initially failed for missing scan playlist publication and `recoverStalePlaylistDetail`; after implementation they passed (`BUILD SUCCESSFUL in 7s`).
- Reconciliation-failure regression initially failed because playlist state was not refreshed on the fail-safe path; after centralizing authoritative publication it passed (`BUILD SUCCESSFUL in 4s`).
- Confirmed-write refresh test was explicitly run RED after removing the prematurely added helper, failing on missing `mutatePlaylistAndRefresh`; the minimal helper then passed (`BUILD SUCCESSFUL in 6s`).
- Review-fix tests initially failed for missing `PlaylistRouteNotice` and stale-notice cleanup; after localized notice rendering and reducer cleanup, `PlaylistStateTest` passed (`BUILD SUCCESSFUL in 12s`).

Lifecycle ordering assertions cover:

```text
reconcile -> read_playlists -> library -> playlists
```

for source removal and clear-library, and:

```text
reconcile -> read_playlists -> publish
```

for rescan publication.

## Verification evidence

### Fresh independent revalidation — 2026-07-17

- Focused Task 4 suite: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 549ms`; 26 actionable tasks: 4 executed, 22 up-to-date).
- Fresh supported matrix after `./gradlew --stop`: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 3s`; 101 actionable tasks: 4 executed, 1 from cache, 96 up-to-date).
- `openspec validate playlist-screen --strict` passed with `Change 'playlist-screen' is valid`.
- English/Chinese XML parity passed with 136 keys in each locale.
- `/usr/bin/xcrun xcodebuild -version` passed with Xcode 26.6, build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` compiled iOS main and then failed at unchanged common-test `Thread` references in `AppScanCancellationTest.kt:64:28` and `:340:27` (`BUILD FAILED in 4s`). No iOS simulator or iOS FK proof pass is claimed.
- `GIT_MASTER=1 git diff --check` passed. The worktree contains only the five pre-existing modified `.superpowers/sdd/` evidence files; no production, test, resource, or OpenSpec file is uncommitted.
- Kotlin LSP diagnostics remain unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation/tests are the executable Kotlin checks.
- This runtime could not execute the `review-work` skill's prescribed Oracle/QA agent types. No new five-lane review pass is claimed; the review evidence below is retained as historical evidence from the implementation session.
- No approved screenshot/reference capture path was available for this minimal Compose surface. Source-level design-system reuse was rechecked, but pixel-level light/dark, compact/wide, and CJK visual acceptance remains deferred to Task 7.

Focused post-commit command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: PASS, `BUILD SUCCESSFUL in 2s`, 110 actionable tasks (5 executed, 105 up-to-date).

Fresh supported matrix before commits:

```bash
./gradlew --stop && ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Result: PASS, `BUILD SUCCESSFUL in 20s`, 101 actionable tasks (12 executed, 89 up-to-date). Existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only.

Additional checks:

- `openspec validate playlist-screen --strict`: PASS, `Change 'playlist-screen' is valid`.
- English/Chinese XML parity script: PASS, 136 keys in each locale.
- `GIT_MASTER=1 git diff --check`: PASS with no output.
- Kotlin LSP diagnostics: unavailable because `kotlin-ls` is not installed and installation was previously declined. Gradle compilation/tests are the executable Kotlin checks; no LSP-clean claim is made.
- `/usr/bin/xcrun xcodebuild -version`: PASS, Xcode 26.6, build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: FAIL after iOS main compilation at unchanged common-test errors:
  - `AppScanCancellationTest.kt:64:28 Unresolved reference 'Thread'.`
  - `AppScanCancellationTest.kt:340:27 Unresolved reference 'Thread'.`
  - `BUILD FAILED in 9s`; no iOS simulator or iOS FK proof pass is claimed.

## Review evidence

Seven read-only review lanes covered goal/scope, hands-on tests, code quality, security, context/history, Compose design-system integrity, and English/Chinese source-level visual fit.

Accepted findings were fixed:

- moved PlaylistRepository reads/writes off the UI dispatcher;
- serialized playlist reads/writes/publications through one mutex owner;
- dispatched loading before retry;
- replaced raw playlist repository exception messages with stable neutral state keys;
- rendered localized stale/mutation notices;
- cleared stale notices after confirmed refresh;
- made retry an explicit 48 dp full-width target with localized semantics.

Targeted post-fix re-review: PASS with no Critical or Important findings. It explicitly excluded Task 5/6 screens and Task 7 integration per the user scope.

Source-level visual QA limitation: no screenshot/device capture was available. The minimal home entry and route scaffold reuse existing Miuix/HausColors/chrome primitives; pixel-level light/dark, compact/wide, and CJK rendering remains Task 7 manual QA.

## Commits

- `086e28c feat: add playlist state model`
- `b99195f feat: add typed playlist routes`
- `fab3cf6 feat: add playlist library entry resources`
- `af43513 feat: own playlist lifecycle state`
- `bc5232b docs: complete playlist navigation tasks`

No push was performed.

## Concerns and deferrals

- `[blocked] iOS FK proof`: the unchanged common-test `Thread` compilation errors prevent `LibraryDatabaseIosTest` from executing. This remains the Task 7 completion gate and is not claimed resolved.
- Task 7.1 still owns database-backed cascade/active-resolved-queue integration proof; Task 4 proves App publication ordering and refresh ownership only.
- Full Saved-tab/detail presentation is Task 5. Full Queue-tab presentation and controls are Task 6.
- The four pre-existing modified files `.superpowers/sdd/progress.md` and `task-1-report.md` through `task-3-report.md` were preserved and never staged in Task 4 commits.
- This `task-4-report.md` is intentionally uncommitted per the user request.

Route: openspec+superpowers / strict RED-GREEN Task 4
Owner: implementation
Input: `.superpowers/sdd/task-4-brief.md`, approved plan/spec/OpenSpec artifacts
Output: five atomic commits plus this uncommitted report
Next owner: Task 5 implementation after Task 4 acceptance
Blockers: unchanged iOS common-test `Thread` references; no JVM, desktop, Android, OpenSpec, resource-parity, diff-hygiene, or focused-review blocker

## Review findings follow-up — 2026-07-17

### Status

All three Task 4 review findings are fixed and committed. OpenSpec 4.1-4.4 were explicitly reopened in `8f49b49`, revalidated after implementation, and re-completed in `141bd35`; Tasks 5-7 remain unchanged and unchecked.

### Root causes and fixes

1. **Serialized publication:** `PlaylistStateOwner` previously serialized repository reads/writes but returned raw snapshots, so callers could delay Compose reduction after the mutex unlocked and apply stale refresh A after mutation B. The owner now assigns a monotonic revision under the same mutex and returns `SnapshotConfirmed`; every App refresh, mutation, scan, source-removal, and clear-library path carries that revision to the reducer, which rejects lower revisions.
2. **Cancellation-safe authoritative publication:** scan/remove/clear cancellation handlers previously called suspending playlist reads and publication in an already-cancelled context. After `ownerIsActive()` authorizes publication, the bounded playlist read plus ordered authoritative publication now runs in `NonCancellable`, then the original `CancellationException` is rethrown.
3. **Stale-detail confirmation:** a default empty snapshot was indistinguishable from a confirmed empty repository. `PlaylistState` now records whether an authoritative snapshot has ever succeeded. An unresolved detail stays on its route while initial loading, initial read failure, or retry loading is active; it returns to the hub only after a settled confirmed absence. Existing resolved playlists remain visible during retry.

Localized notices, typed route and Now Playing policies, adaptive behavior, exact `PlaylistEntry.id` occurrence mapping, and Task 5/6 UI boundaries are unchanged.

### Strict RED evidence

- Publication/detail RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache` failed at test compilation on missing `hasConfirmedSnapshot`, `AwaitConfirmation`, the state-based resolver, and owner-issued `SnapshotConfirmed` actions (`BUILD FAILED in 5s`).
- Cancellation RED: the three focused cancellation tests compiled and all failed behaviorally because only `reconcile` occurred; the yielding playlist read and publication were skipped in the cancelled context (`3 tests completed, 3 failed`, `BUILD FAILED in 5s`).
- Retry-detail RED: `PlaylistStateTest.unresolvedPlaylistDetailWaitsForAuthoritativeConfirmation` failed because a previously confirmed empty snapshot returned to the hub while retry loading was active (`1 test completed, 1 failed`, `BUILD FAILED in 3s`).

### GREEN and verification evidence

- Revisioned publication and confirmed-absence state: complete `PlaylistStateTest` passed (`BUILD SUCCESSFUL in 10s`). The deterministic race pauses refresh A after it receives the old revision, completes and publishes mutation B, then releases A and proves B remains.
- Cancellation ordering: the three focused cancellation regressions passed (`BUILD SUCCESSFUL in 5s`) and assert:
  - rescan: `reconcile -> read_playlists -> publish`;
  - source removal and clear: `reconcile -> read_playlists -> library -> playlists`;
  - each operation rethrows `CancellationException` after publication.
- Focused Task 4 suite before commit: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 5s`).
- Complete shared JVM suite: `./gradlew :shared:jvmTest --configuration-cache` passed (`BUILD SUCCESSFUL in 10s`; 26 actionable tasks: 8 executed, 18 up-to-date).
- Supported compile/assemble matrix: `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 9s`; 105 actionable tasks: 10 executed, 95 up-to-date; unchanged Android artwork deprecation warning only).
- Post-commit focused Task 4 suite passed (`BUILD SUCCESSFUL in 754ms`; 26 actionable tasks: 5 executed, 21 up-to-date).
- `openspec validate playlist-screen --strict`: PASS before and after commits (`Change 'playlist-screen' is valid`).
- English/Chinese resource parity: PASS, 136 keys each.
- `GIT_MASTER=1 git diff --check`: PASS.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable language checks.

### Follow-up commits

- `8f49b49 docs: reopen playlist navigation tasks`
- `95641e8 fix: serialize playlist state publication`
- `141bd35 docs: revalidate playlist navigation tasks`

Every commit uses the repository's semantic English style plus the required Sisyphus footer and co-author trailer. No push was performed.

### Remaining concerns

- The existing unrelated iOS common-test `Thread` blocker and Task 7 iOS FK proof gate are unchanged; this follow-up does not claim iOS simulator execution.
- Task 7 database-backed cascade/active-queue integration and manual visual/device QA remain out of scope.
- `.superpowers/sdd/task-4-report.md` remains intentionally uncommitted. The pre-existing dirty `.superpowers/sdd/progress.md` and Task 1-3 reports were preserved and never staged.

## Terminal outcomes and playlist-read failure follow-up — 2026-07-17

### Status

The remaining Task 4 findings are fixed and committed. OpenSpec 4.1-4.4 were reopened in `2618d5a` and re-completed in `943d94a`; Tasks 5-7 remain unchanged and unchecked.

### Root causes and coherent fix

1. `SnapshotConfirmed` carried the owner revision, but `ReadFailed` and `MutationFailed` were created by App after the serialized owner operation threw. A delayed old failure could therefore clear newer loading, add a stale error/notice, or overwrite the terminal status associated with a newer snapshot.
2. Scan/remove/clear placed reconciliation, playlist read, and publication inside one catch boundary. A playlist read exception was therefore treated as reconciliation failure; the catch attempted another playlist read and could suppress authoritative library or terminal scan publication after the source mutation had already committed.

`PlaylistStateOwner` now increments one monotonic revision under its mutex for every non-cancelled operation and returns exactly one terminal `PlaylistStateAction`: `SnapshotConfirmed`, `ReadFailed`, or `MutationFailed`. The reducer rejects every terminal action older than its latest accepted revision. App no longer manufactures unversioned read/mutation failures outside the owner.

Lifecycle helpers now complete reconciliation classification first, perform exactly one playlist outcome read second, and publish authoritative library/scan state third. A playlist read failure is a revisioned retryable playlist action, so the reducer retains the last confirmed playlist snapshot while exposing `playlist_load_failed`. It is not a generic reconciliation/scan failure and is never retried by a catch. Active-owner cancellation still performs one playlist read plus authoritative publication inside the existing bounded `NonCancellable` block and rethrows the original cancellation.

Routes, localized notices/resources, exact `PlaylistEntry.id` occurrence mapping, Now Playing/adaptive policies, and Task 5/6 UI scope remain unchanged.

### Strict RED evidence

- Command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`.
- Result: expected compilation failure (`BUILD FAILED in 1s`) because `PlaylistStateOwner.refresh(failureMessage)` and `mutate(failureMessage, mutation)` did not exist; lifecycle failure tests could not obtain revisioned terminal actions.
- The added deterministic state regressions retain an old refresh/mutation failure action, complete and reduce a newer success, then release/reduce the old failure. They require the old read failure not to clear newer loading or add a read error, and the old mutation failure not to add a recoverable notice.
- The added remove/clear/rescan regressions use a counting playlist repository that always throws on read. They require authoritative library or terminal scan publication, retention of the previously confirmed playlist, one revisioned retryable read error, and exactly one read attempt.

### GREEN and verification evidence

- Focused owner/lifecycle GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 2s`; 26 actionable tasks: 6 executed, 20 up-to-date).
- Complete shared JVM suite passed: `./gradlew :shared:jvmTest --configuration-cache` (`BUILD SUCCESSFUL in 3s`; 26 actionable tasks: 5 executed, 21 up-to-date).
- Pre-commit focused Task 4 suite passed: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache` (`BUILD SUCCESSFUL in 2s`; 26 actionable tasks: 6 executed, 20 up-to-date).
- Supported matrix passed: `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` (`BUILD SUCCESSFUL in 10s`; 96 actionable tasks: 10 executed, 86 up-to-date; unchanged Android artwork deprecation warning only).
- Post-commit focused Task 4 suite passed (`BUILD SUCCESSFUL in 551ms`; 26 actionable tasks: 4 executed, 22 up-to-date).
- `openspec validate playlist-screen --strict`: PASS before and after commits (`Change 'playlist-screen' is valid`).
- English/Chinese resource parity: PASS, 136 keys each.
- `GIT_MASTER=1 git diff --check`: PASS.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable Kotlin checks.

### Commits

- `2618d5a docs: reopen playlist navigation outcomes`
- `f35b71b fix: order playlist terminal outcomes`
- `943d94a docs: revalidate playlist terminal outcomes`

Every commit uses semantic English style plus the required Sisyphus footer and co-author trailer. No push was performed.

### Remaining concerns

- The unchanged iOS common-test `Thread` blocker and Task 7 iOS FK proof gate remain outside this Task 4 review follow-up; no iOS simulator pass is claimed.
- Task 7 database-backed integration and manual visual/device QA remain unchecked and out of scope.
- This report remains intentionally uncommitted. The pre-existing dirty SDD progress and Task 1-3 reports were preserved and never staged.
