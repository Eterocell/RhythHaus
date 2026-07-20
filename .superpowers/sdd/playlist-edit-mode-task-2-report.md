# Playlist edit mode Task 2 report

## Repair review

Repairing Oracle review `ses_080411802ffeYeEnV9sKlglRV7` without expanding scope.

### RED evidence

Added production `PlaylistDetailScreen` behavior tests before repair implementation:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: expected `BUILD FAILED`; the new interaction assertions exposed that edit controls were not driven by the callback-cleared state and that the modal registration test could not observe stable production ownership.

### GREEN evidence

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --configuration-cache --rerun-tasks
```

Both returned `BUILD SUCCESSFUL`.

### Finding resolutions

## Completion repair

### RED evidence

The new owner/modal and delete-order regression tests were introduced before implementation and the focused command failed at test compilation because the production completion helper had not yet been given its callable return contract.

Command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: expected `BUILD FAILED` during test compilation (`Cannot infer type ... completeDelete()`), before the implementation correction.

### GREEN evidence

The same focused command returned `BUILD SUCCESSFUL` after the production helper and lifecycle wiring were corrected.

### Additional finding resolutions

5. Delete success now uses the explicit `playlistDeleteCompletion` callback. It dismisses confirmation, clears selection, and only then invokes the route-pop completion callback; it does not re-enter the decision-based Back closure. The regression test asserts exact `dismiss`, `selection`, `pop` ordering.
6. `PlaylistBackRegistrationState` is the production owner-aware registration holder used by `LibraryAppShell`; JVM tests now exercise the actual helper for stale edit and stale modal disposers, rather than a test-only duplicate implementation.
7. Existing production `PlaylistDetailScreen` tests continue to verify edit-row visibility and unregister behavior; the unified shell policy preserves modal > edit > selection > now playing > pop precedence and predictive progress remains limited to `PopRoute`.

1. Edit registration now keys on actual `editMode`; row rendering also uses actual state. The shell clear callback sets `editMode = false`, and `DisposableEffect(editMode, editOwner)` unregisters immediately with owner-safe disposal.
2. Successful delete now closes confirmation first, then uses a state-owned `deleteRoutePending` effect to invoke `onDeleteCompleted`/route Back after modal ownership is gone. User Back still uses unified shell precedence.
3. Modal registration uses a stable `modalOwner`, `rememberUpdatedState`, and an effect keyed by modal presence/owner instead of the freshly-created dismiss lambda.
4. JVM Compose tests now exercise production `PlaylistDetailScreen` registration behavior, actual edit clearing/unregistration, and no playback/route side effects for a state-only modal branch. The previous fake duplicate registration test remains only as the pure owner-race guard for shell identity semantics.

## RED

Added the required precedence assertions to `LibraryNavigationTest.kt` before production changes:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache --rerun-tasks
```

Expected RED: `compileTestKotlinJvm FAILED` with unresolved `DismissPlaylistModal`/`ExitPlaylistEditMode` and the old three-argument `libraryBackDecision` signature.

## GREEN

Focused policy and registration tests passed:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --configuration-cache --rerun-tasks
```

Both returned `BUILD SUCCESSFUL`.

## Changes

- Extended `LibraryBackDecision` and `libraryBackDecision` with modal then edit precedence.
- Added shell-owned edit/modal callback registration with owner-safe edit disposal.
- Forwarded registration callbacks through `LibraryRouteContent` to `PlaylistDetailScreen`.
- Routed playlist frame Back and predictive system Back through the shell decision.
- Gated predictive progress to `PopRoute`.
- Added pure precedence and owner-race tests.

## Commit

Committed with the requested message:

```text
feat: unify playlist back handling
```

## Concerns

- Full repository verification was not run; focused JVM tests only.
- `.cortexkit/` remains untouched and untracked.
