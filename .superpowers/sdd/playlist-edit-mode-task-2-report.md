# Playlist edit mode Task 2 report

## Corrective RED evidence

Added production-boundary registration observability and real modal interaction assertions before the corrective implementation:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: expected `BUILD FAILED`. The first run failed at test compilation because the observable registration properties did not yet exist. After that test-shape correction, the real Compose tests failed behaviorally: the edit-clear assertion could not observe the expected production row transition, and the modal test could not find the intended live action node. This was not treated as compiler-only RED.

## Corrective GREEN evidence

The final focused command returned `BUILD SUCCESSFUL`:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Fresh result: 8 tests completed, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL in 8s`.

## Finding resolutions

- `PlaylistBackRegistrationState` now stores edit/modal registrations in Compose snapshot state, so child `DisposableEffect` mutations invalidate the shell and recompute the production back decision / `NavigationBackHandler.isBackEnabled`. Existing owner-and-callback identity guards remain intact.
- Delete success now closes the confirmation state, then calls dedicated `onDeleteCompleted`. `LibraryHomeScreen` supplies that callback through `LibraryRouteContent` and performs clear-selection followed by direct `appState.popRoute()`; it does not re-enter decision-based Back dispatch.
- Production `PlaylistDetailScreen` tests prove edit controls exist before registered clear, disappear afterward, and real rename-modal registration dismisses the modal without playback or route side effects.
- The existing policy preserves modal > edit > selection > now playing > pop precedence, with predictive progress limited to `PopRoute`.

## Corrective changes

- Made edit/modal registration fields Compose-observable while preserving stale disposer identity guards.
- Threaded a dedicated direct delete-completion callback from `LibraryHomeScreen` through `LibraryRouteContent` to `PlaylistDetailScreen`.
- Invoked delete completion instead of constructing and discarding a lambda.
- Added production-boundary JVM behavioral coverage for registration observability and real modal dismissal.

## Commit

Corrective commit:

```text
fix: make playlist back state observable
```

## Concerns

- Focused JVM verification only; the broader repository matrix was not run in this corrective pass.
- `.cortexkit/` remains untouched and untracked.
