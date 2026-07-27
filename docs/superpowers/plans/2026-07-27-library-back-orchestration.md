# Library Back Orchestration Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use `superpowers:subagent-driven-development` (recommended) or `superpowers:executing-plans` to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `LibraryAppState` the one shared, destination-scoped owner of Library Back resolution, latched Back sessions, and exact displayed-playlist invalidation. Ordinary interaction, system Back, and predictive Back must adapt that one protocol without retargeting, completion-time resolution, or deletion-as-Back behavior.

**Architecture:** Add small destination/target/session value models and a narrow `LibraryBackSurfacePort` adjacent to `LibraryAppState`, not another coordinator, registry, dependency, schema, or platform abstraction. The shell feeds the *presented* route's instance identity to state; an active feature publishes only its foremost modal/edit target. `TrackSelectionState` remains shell/feature owned. A session latches one target and route preview; only exact authoritative-state observation or explicit rejection clears it. Playlist deletion directly invalidates an exact destination and never enters Back resolution.

## Global constraints

- This plan owns OpenSpec `architecture-refactor` continuation Tasks 7–12. Preserve current product/navigation behavior, visual behavior, adaptive layout, playback, persistence, dependencies, and toolchains.
- Strict TDD applies to every production slice: write the named test, run and record the expected focused RED, make the minimal GREEN change, rerun GREEN, then begin the next slice. A compilation RED is valid only when the missing deliberately introduced symbol causes it.
- Every task must leave the project compiling. Use the focused test command after each slice and `:shared:compileKotlinJvm` after state/shell composition changes.
- `LibraryDestinationId` combines stable route identity **and an instance token**; route equality or callback identity is inadequate. `LibraryBackTargetId` includes destination and concrete feature/module target instance identity. Same-shaped route/target replacement must receive fresh identities.
- Active destination means only the current navigation target presented to the user. Retained/outgoing `AnimatedContent`, predictive preview, hidden overlay, and stale composition must not become eligible merely because it is composed or registered.
- Feature surfaces own local modal ordering and edit state. The module accepts one foremost action for an active destination; it does not build a global modal stack.
- `beginBack()` is the sole decision point: modal → edit → selection owned by the active eligible page → Now Playing → route → unhandled root. Completion never re-resolves, retargets, or falls through.
- A session remains pending after `complete()` returns. Callback return is not settlement. Repeated ordinary/system/predictive starts are suppressed until exact observed settlement or explicit `reject()`. Rejection allows a later fresh request but never falls through in the rejected session.
- Predictive start holds only the returned session. Progress/preview read that session's precomputed route preview; cancel performs no transition; completion calls the same session and never resolves again.
- Root must return unhandled without Library mutation. The invoking adapter retains its interaction/platform default; do not convert root into a consumed no-op.
- Keep `TrackSelectionState` shell/feature owned. It can be a Back target only when its page key equals `trackSelectionPageKeyFor(activeRoute, browseMode)`; stale selection is reconciled normally and never app-wide Back state.
- Displayed-playlist invalidation is separate from Back. It preserves Now Playing, unrelated selection/route/feature state, does not call `beginBack()`, and never dispatches/completes a Back target.

## Exact implementation and test files

**Production**

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt`: make `LibraryAppState` own active destination, accepted port, session, authoritative reconciliation, and exact playlist invalidation. Retain `rememberLibraryAppState(snapshot)` lifecycle behavior.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`: add only pure identity/resolution/route-preview/selection-eligibility helpers. Retain `libraryBackDecision` only if it is internal to the single new resolver; do not leave a shell-callable second policy.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`: replace shallow registration/controller/callback factories and duplicate shell-local decision/request closures with the state module protocol. Feed active *presented* destination identity and retain a predictive session for one gesture.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`: forward destination ID and feature port seam to playlist detail; wire authoritative deletion confirmation to exact invalidation rather than `onDeleteCompleted`.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt`: migrate feature-owned playlist modal/edit publication to one destination-scoped port with stable feature target IDs. Inspect current surface before editing; keep feature ordering/state local.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackRegistrationState.kt`: delete it if no longer useful, or reduce/rename it to a feature-owned port publisher only. Remove from it: `PlaylistBackDispatchController`, `libraryBackCompletionCallback`, `LibraryBackCallbacks`, `libraryBackCallbacks`, and `directPlaylistDeleteCompletion`.

**Tests**

- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`: primary pure/module contract test surface; split to an adjacent `LibraryBackSessionTest.kt` only if this makes the focused state-machine tests materially clearer.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackPolicyJvmTest.kt`: replace shallow-helper tests with JVM/Compose destination-publication and stale-disposer wiring coverage.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistEditModeSemanticsJvmTest.kt`: migrate actual playlist modal/edit/system/delete interaction coverage to real `LibraryAppState` protocol wiring.

**Focused commands**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

---

### Task 7: RED destination, target, and pure resolution contract

**Interfaces:** introduce test-first names/shapes for `LibraryDestinationId`, `LibraryBackTargetId`, `LibraryBackTarget`, `LibraryBackSurfacePort`, and a pure resolver/begin result (`Unhandled`, `Suppressed`, `Started(session)` or equivalent). Exact Kotlin names may follow project conventions, but all stated identity/lifetime semantics are mandatory.

- [x] **Step 1: Add precedence RED tests in `LibraryNavigationTest.kt`.** Construct fake destination/port/authoritative inputs and assert modal, edit, active-page selection, Now Playing, route, then root-unhandled. Assert only one target is returned and it carries its exact destination and target instance IDs.
- [x] **Step 2: Add locality RED tests.** Prove a modal/edit port published by inactive, hidden, outgoing, or stale destination cannot resolve. Prove the feature supplies an already-chosen foremost target and the module never orders feature modal internals. Prove selection for a page other than `trackSelectionPageKeyFor(activeRoute, browseMode)` cannot consume Back.
- [x] **Step 3: Add replacement identity RED tests.** Resolve playlist detail `playlist-1` at destination token A with modal instance A; replace with equal route at destination token B/modal instance B. Assert destination/target IDs differ and B cannot satisfy the A target/session.
- [x] **Step 4: Run RED:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected RED: compilation fails only because the new identity/resolution contract does not exist, or assertions fail only because no resolver behavior exists. Do not wire adapters.
- [x] **Step 5: Implement minimal pure models/resolver in `LibraryNavigation.kt` or adjacent to `LibraryAppState.kt`.** Derive route identity explicitly; create instance token at state/shell lifecycle, not callback references. Add target variants for feature modal/edit, page selection, Now Playing, and route. Precompute `navigation.pop()` route preview without mutating navigation.
- [x] **Step 6: GREEN and compile checkpoint:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Review: no adapter, callback-factory, state migration, or selection-ownership change in this task.

---

### Task 8: `LibraryAppState` latching, settlement, rejection, and destination port publication

**Interfaces:** `LibraryAppState` owns active destination identity, accepted active `LibraryBackSurfacePort`, and `LibraryBackSession?`. A port is accepted only if its destination equals the active destination; its disposer clears only the exact registration it created. Sessions expose target, route preview, `complete`, `cancel`, `reject`, and authoritative reconciliation.

- [x] **Step 1: Add lifecycle RED tests in `LibraryNavigationTest.kt`.** Use fake target execution that returns without changing state. Assert `beginBack()` latches exactly one target, second begin is suppressed, complete executes once, repeated complete cannot execute again, callback return leaves session pending, and lower-precedence state remains unchanged.
- [x] **Step 2: Add settlement/rejection RED tests.** Change authoritative target state after dispatch and reconcile it. Assert only disappearance/ineligibility of the exact latched target releases suppression. Assert `reject()` releases without transition/fall-through and a later begin resolves fresh state. Assert a new higher-priority target cannot steal or settle an old pending target; route replacement invalidates old session instead of completing a new route target.
- [x] **Step 3: Add publication/stale-disposer RED tests.** Register port A, replace it with current port A2 or destination B, then invoke A disposer. Assert the newer accepted registration remains. Attempt inactive registration while A is active and assert it is ignored. Change active destination and assert prior feature port is no longer consumable. Assert module dispatches only the feature's single published target/rejection seam.
- [x] **Step 4: Run RED:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected RED: lifecycle/publication/authoritative-settlement APIs are missing. Do not adapt `PlaylistBackDispatchController` as an implementation.
- [x] **Step 5: Minimal GREEN implementation in `LibraryAppState.kt`.**
  - Add active-destination feed at shell presentation boundary. On identity change, invalidate only old-destination session/eligibility; do not clear unrelated registrations/state.
  - Add identity-checked `registerBackSurface(port): () -> Unit`, accepting only `port.destinationId == activeDestinationId`; disposal compares exact registration token before clearing.
  - `beginBack()` snapshots authoritative inputs and either returns unhandled without mutation or retains one session. `complete()` revalidates only its latched exact target and invokes it at most once. `cancel()` releases uncompleted predictive session without transition. `reject()` releases without fall-through.
  - Reconcile only from authoritative inputs after real state changes. Never treat callback return as settlement; no global modal stack; selection stays an input from its existing owner.
- [x] **Step 6: GREEN and review exact-one target execution:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

Verify execution is exactly one of feature modal/edit, shell selection cancellation, `hideNowPlaying`, or precomputed route application—never resolution again or incidental selection clear.

---

### Task 9: RED ordinary/system/predictive adapter-equivalence contract

**Interfaces:** narrow common adapter-facing seams over `beginBack()`/`LibraryBackSession`, testable with fake state/session. Adapters have equivalent handled semantics while retaining their own unhandled default.

- [x] **Step 1: Add ordinary/system equivalence RED tests in `LibraryNavigationTest.kt`.** For modal, edit, active selection, Now Playing, route, and root, assert both adapters begin same target and call complete once when handled; pending second input is suppressed; no target invocation occurs when suppressed; root invokes only adapter-local default.
- [x] **Step 2: Add predictive RED tests.** Begin route target, capture precomputed preview, alter route/precedence, issue progress and complete; assert preview stays latched and completion does not resolve again. Assert cancellation causes zero execution. Assert invalid completion (target disappears/replaced) is no-op/settlement without fall-through. Assert unchanged valid completion invokes exactly once then awaits/observes settlement.
- [x] **Step 3: Cover mandatory lifecycle races.** Callback return before modal/edit state changes remains pending; explicit feature rejection releases suppression; stale target plus a new higher-priority target cannot cause completion to execute the new one; same-shaped route replacement cannot satisfy old route target; root remains unhandled.
- [x] **Step 4: Run RED:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Expected RED: adapter protocol is absent or existing completion-time decision behavior fails. Do not edit `NavigationBackHandler` yet.
- [x] **Step 5: Add only thin common adapter support needed for GREEN.** Ordinary/system calls begin and complete a started session once. Predictive retains the exact session and reads only `session.routePreview`; no resolver is passed to completion. Keep platform wiring for Task 10.
- [x] **Step 6: GREEN:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
```

Review: adapter tests prove one protocol, not a copied policy table.

---

### Task 10: Destination-scoped feature wiring, real adapters, and shallow-helper deletion

**Interfaces:** `LibraryAppShell` assigns one destination ID per presented route instance; `LibraryRouteContent`/playlist detail publish via a destination-scoped port; toolbar/in-app, system, and predictive Back use `LibraryAppState` sessions. Feature port publishes feature-owned stable modal/edit IDs and execution/rejection only.

- [x] **Step 1: Add JVM Compose RED tests in `PlaylistBackPolicyJvmTest.kt`.** Replace every test bound to `PlaylistBackRegistrationState`, `PlaylistBackDispatchController`, `libraryBackCompletionCallback`, `libraryBackCallbacks`, or `directPlaylistDeleteCompletion`. Mount playlist detail with real `LibraryAppState` and prove: only active destination consumes; stale disposer cannot clear current A/A2/B registration; hidden/outgoing destination cannot consume; feature-selected foremost modal precedes edit; callback return with unchanged state keeps a repeated request suppressed.
- [x] **Step 2: Add/migrate interaction RED tests in `PlaylistEditModeSemanticsJvmTest.kt`.** Migrate real rename modal, toolbar/system precedence, and predictive behavior to the real state protocol. Assert no modal/edit completion clears selection/hides Now Playing/pops route; predictive cancel has no transition; valid route gesture uses one latched preview/action; no completion-time resolution occurs.
- [x] **Step 3: Run RED:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
```

Expected RED: current shallow helpers cannot provide destination/session semantics. Keep unrelated playlist edit/row semantics assertions intact.
- [x] **Step 4: Feed active identity in `LibraryAppShell.kt`.** Create fresh destination token when presented top route is replaced, including equal route values; do not let outgoing/preview `RouteContent` overwrite active identity. Keep `rememberLibraryAppState(snapshot)` on its current `snapshot.nowPlayingTrackId` key so normal recomposition/gesture progress does not recreate module state.
- [x] **Step 5: Migrate feature publication.** Pass active destination ID/capability through `LibraryRoutes.kt` to `PlaylistScreens.kt`. Playlist feature creates stable edit-session/modal-instance IDs, chooses/publishes only foremost local action, and identity-checks disposal. Feature keeps its own dialog/edit state and provides rejection when it cannot execute; authoritative absence/change settles.
- [x] **Step 6: Migrate adapters incrementally.**
  - [x] Toolbar/in-app Back begins then completes one session; unhandled keeps existing local default.
  - [x] `NavigationBackHandler` uses identical begin/complete protocol; root remains delegated/default rather than a Library consumed no-op.
  - [x] Predictive begins once, retains session until cancel/complete, draws offset/preview only from latched route preview, resets after cancel/settlement, and calls only stored session complete. Remove `backGestureProgressAtCompletion`/`completePredictivePop` plumbing if no longer needed; apply precomputed route state rather than calling `navigation.pop()` at completion.
  - [x] Reconcile after real modal/edit/selection/Now Playing/route state changes so only exact target settles.
- [x] **Step 7: Delete replacements.** Remove `PlaylistBackDispatchController`, `libraryBackCompletionCallback`, `LibraryBackCallbacks`, `libraryBackCallbacks`, and `directPlaylistDeleteCompletion`. Delete `PlaylistBackRegistrationState` or reduce/rename it to feature-port publication only. Remove duplicate shell-local `libraryBackDecision`/`requestLibraryBack` closures and direct Back clear/pop paths. Retain pure `libraryBackDecision` only if sole module resolver uses it internally.
- [x] **Step 8: GREEN after each adapter slice, then run group:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

- [x] **Step 9: Search/review changed production paths.** No removed symbol may remain. Any direct `popRoute`, selection clear, or callback completion must be ordinary navigation/state work or module-latched application—not a Back policy bypass.

---

### Task 11: Exact displayed-playlist invalidation (not Back)

**Interfaces:** add named `LibraryAppState` invalidation taking authoritative playlist-deletion/existence confirmation plus active destination. It affects only active `LibraryRoute.PlaylistDetail(playlistId)` with exact displayed instance/playlist match and returns/replaces it with playlist hub using existing behavior.

- [x] **Step 1: Add common RED exact-invalidation test in `LibraryNavigationTest.kt`.** Begin active `PlaylistDetail("playlist-a")` with its port/edit/modal/selection and pending session, plus visible Now Playing and unrelated state. Confirm authoritative deletion of `playlist-a`. Assert only this detail leaves/replaces to `PlaylistHub`; its port/page selection/session disappear; the matching pending session settles invalid without dispatching another target; Now Playing and unrelated state survive.
- [x] **Step 2: Add common RED no-op companions.** Delete `playlist-b` while A displays; delete playlist A while another destination is active; report playlist still existing. Assert no navigation, no unrelated selection clear, no port removal, no session begin/complete, no Back consumption.
- [x] **Step 3: Add JVM Compose deletion RED in `PlaylistEditModeSemanticsJvmTest.kt`.** Replace `successfulDeleteUsesShippingCompletionWithModalPrecedenceAndDirectPop`. Use existing mutation/snapshot confirmation to prove exact state invalidation; assert no `onDeleteCompleted`, broad selection clear, ordinary Back dispatch, completion-time pop, or stale delete modal, while unrelated harness state remains observable.
- [x] **Step 4: Run RED:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
```

Expected RED: current direct delete completion clears selection and pops, violating exact invalidation/Back separation.
- [x] **Step 5: Minimal GREEN implementation.** In `LibraryAppState`, compare confirmed ID with active route and instance. Atomically discard only that destination port/page-owned selection/session, invalidate matching session without dispatch, and replace/return that exact detail to hub. Do not alter generic stale-detail recovery until callers distinguish it from exact invalidation.
- [x] **Step 6: Wire `LibraryRoutes.kt`.** Invoke invalidation only once playlist state/repository snapshot confirms absence. Remove `onDeleteCompleted` from `LibraryRouteContent` and playlist detail once unused. Local delete modal closes through feature state/authoritative outcome, not a Back callback.
- [x] **Step 7: GREEN and review:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache
./gradlew :shared:compileKotlinJvm --configuration-cache
```

The final deletion call path must not reference `beginBack`, Back factories, direct selection clear, or generic direct pop.

---

### Task 12: OpenSpec evidence and supported-platform verification

- [x] **Step 1: Strict OpenSpec validation:**

```bash
openspec validate architecture-refactor --strict
```

Expected: `Change 'architecture-refactor' is valid`. Correct only continuation evidence required by a failure.
- [x] **Step 2: Fresh focused matrix:**

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistBackPolicyJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistEditModeSemanticsJvmTest' --configuration-cache --rerun-tasks
```

Expected: zero failures, errors, and skips; inspect reports for any skip.
- [x] **Step 3: JVM, desktop, Android:** Final aggregate verification passed on 2026-07-27 after the earlier provisional desktop-compilation blocker was resolved outside this evidence-only closure.

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Expected: `BUILD SUCCESSFUL`; failures are blockers unless exact out-of-scope evidence is recorded.
- [x] **Step 4: Xcode and iOS:**

```bash
/usr/bin/xcrun xcodebuild -version
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Record exact Xcode/Gradle result. If unavailable/failing, record command/output blocker and do not claim iOS pass.
- [x] **Step 5: Final diff/source review:**

```bash
git diff --check
git status --short
git diff -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppState.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistBackRegistrationState.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/PlaylistScreens.kt
```

Confirm: no dependency/schema/toolchain/platform expansion; removed helpers absent; active identity/target identities are instance-specific; callback return does not settle; stale/new higher-priority target cannot retarget; cancellation/root behavior is correct; selection remains shell/feature owned; and deletion remains exact non-Back invalidation.
- [x] **Step 6: Synchronize durable evidence only after results exist.** Update OpenSpec Tasks 7–12, `progress.md`, and `roadmap.md` with route `openspec+superpowers`, owner, changed production/test paths, exact command outcomes, coverage, iOS result, runtime/manual gaps, next owner, blockers, and commit status. Do not claim untested visual/device behavior. Final evidence was reconciled on 2026-07-27; the historical Task 10 standalone-RED-transcript caveat remains recorded in `progress.md`.
- [ ] **Step 7: Before any authorized task commit, inspect `git status --short`, `git diff`, and `git log --oneline -10`; stage only intended files.** Commit only when authorized, using e.g. `refactor: centralize library back sessions`.

## Final acceptance checklist

- [x] `LibraryAppState` owns active-destination resolution, pending session lifecycle, and exact displayed-playlist invalidation.
- [x] Stable destination/target instance identities block stale/replaced registration/session execution or settlement.
- [x] One begin selects one target in modal → edit → active-page selection → Now Playing → route order; root is unhandled.
- [x] Repeated input suppresses until exact observed settlement or explicit rejection; callback return alone never releases it.
- [x] Ordinary/system/predictive paths share one protocol; predictive preview and completion use one latched session; cancellation/invalid completion never transitions or falls through.
- [x] Feature modal ordering/edit state remains feature-owned; destination-scoped stale disposal cannot clear a newer publication.
- [x] `TrackSelectionState` remains shell/feature-owned and active-page scoped.
- [x] Exact displayed playlist deletion invalidates only its destination, preserves unrelated state, and never acts as Back.
- [x] Legacy controller/callback/delete helpers and duplicate shell decision/request paths are removed/replaced by the single module seam.
- [x] Focused and supported-platform outcomes are fresh and recorded in OpenSpec/progress/roadmap evidence. Automated results are passing; live desktop/Android/iOS Back, predictive-gesture, deletion, and visual/interaction behavior remains manual follow-up evidence.
