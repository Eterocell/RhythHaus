# Playlist edit mode Task 2 report

## Corrective RED evidence

Added production-boundary registration observability and real modal interaction assertions before the corrective implementation:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: expected `BUILD FAILED`. The first run failed at test compilation because the observable registration properties did not yet exist. After that test-shape correction, the real Compose tests failed behaviorally: the edit-clear assertion could not observe the expected production row transition, and the modal test could not find the intended live action node. This was not treated as compiler-only RED.

## Integration coverage repair RED evidence

Added the production dispatch-controller seam, real toolbar-back interaction, snapshot observer, and successful-delete production-path assertions before the final repair. The focused command reached behavioral RED with two failing assertions in `PlaylistEditModeSemanticsJvmTest`: the toolbar test initially did not reach the live registration path, and the delete test initially targeted the wrong localized action node. This was an actual assertion failure, not a type-check-only failure.

## Final dispatch repair RED/GREEN evidence

Added assertions for the production `onSystemBackCompleted` callback and the single direct-pop primitive before final implementation. The first targeted run failed at test compilation because the callback and `directPopRoute` seam were not yet present. After the seam was introduced, the real Compose system-back test reached an assertion-level failure while invoking the toolbar path; the test was corrected to invoke the production callback passed to `NavigationBackHandler`, then the final implementation unified direct route pop and deletion completion.

## Corrective GREEN evidence

The final focused command returned `BUILD SUCCESSFUL`:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Fresh result: 8 tests completed, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL in 8s`.

Integration repair result: 12 tests completed, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL in 9s`.

Final dispatch result: 13 tests completed, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESSFUL in 8s`.

## Predictive completion repair evidence

RED command (new predictive callback assertions, before the production callback factory existed):

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest.productionNavigationBackCallbackUsesPredictivePopOnlyForPopRoute' --configuration-cache --rerun-tasks
```

Result: `BUILD FAILED` during test compilation because `libraryBackCompletionCallback` was unresolved. The test assertions were then completed against the production factory, including an assertion-level check that modal/edit decisions never call predictive navigation.

GREEN command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL`; all targeted Task 2 tests passed.

Final integration coverage command:

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --configuration-cache --rerun-tasks
```

The first GREEN attempt reached an assertion-level failure in the new toolbar flow (`expected <3> but was <4>` from counting setup callback invocations). The assertion was corrected to count only the later selection/Now Playing/PopRoute progression. The rerun passed all 16 focused tests.

## Final entry-coverage repair evidence

The final repair replaces the former direct `PlaylistBackDispatchController.onSystemBackCompleted` test with a real `LibraryBackCallbacks.systemBackCompleted` callback produced by the shipping `libraryBackCallbacks(...)` factory, while a real `PlaylistDetailScreen` owns edit and modal registration. The toolbar flow now drives selection cancel, Now Playing hide, and PopRoute through actual `playlist-back` clicks rather than direct callback invocation. The focused rerun passed both Task 2 classes: 16 tests, 0 failures, 0 errors, 0 skipped.

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
- Added `PlaylistBackDispatchController`, used by shell system Back and predictive completion routing; toolbar Back receives the same production dispatch callback through `PlaylistDetailScreen`.
- Removed the unused modal-aware delete helper test and replaced it with the real `PlaylistDetailScreen` successful-delete callback path.
- Added a Compose recomposition observer proving registration changes update the derived production decision.
- Unified route popping behind `directPopRoute()`, which performs exactly one selection clear and one `appState.popRoute()`. Both PopRoute dispatch and the dedicated delete completion use this primitive; predictive completion remains PopRoute-only.
- Added and wired the production `onSystemBackCompleted` callback directly into `NavigationBackHandler`; JVM coverage invokes that same callback through the real screen registration path.
- Restored predictive PopRoute completion through the exact callback installed in `NavigationBackHandler`: capture progress, clear selection once, call `navigation.pop()`, call `completePredictivePop(next)`, then clear completion progress. Modal/edit branches use ordinary dispatch and never predictively pop.
- Shipped delete completion now comes from the shell's direct completion factory and is passed unchanged through `LibraryRouteContent`.
- `LibraryBackCallbacks` is now the shipping shell fixture: `LibraryAppShell` instantiates it, passes its ordinary callback to playlist toolbar/UI content, passes its delete callback through `LibraryRouteContent`, and passes its exact `systemBackCompleted` callback to `NavigationBackHandler`. New UI tests use this same production factory for modal/edit and delete integration coverage.

## Commit

Corrective commit:

```text
fix: make playlist back state observable
```

Integration coverage commit:

```text
test: cover playlist back integration
```

## Concerns

- Focused JVM verification only; the broader repository matrix was not run in this corrective pass.
- `.cortexkit/` remains untouched and untracked.
