# Task 5 Report: Playback Session FIFO Coordinator

## Scope

- Implemented only the Task 5 coordinator and focused common tests.
- Preserved the review-clean Task 1-4 controller, store, platform engines, dependencies, lifecycle wiring, UI, SQL, OpenSpec, progress, and roadmap files.
- Existing unrelated modifications to `.superpowers/sdd/task-1-report.md` and `.superpowers/sdd/task-2-report.md` were left untouched.

## Root design

- `PlaybackSessionCoordinator` owns one unlimited FIFO `Channel<Command>` and one process-scope actor coroutine.
- Commands are `Restore`, `Checkpoint`, `Reconcile`, and `Flush`.
- Only an immediately adjacent run of `Checkpoint` commands is drained and collapsed to its newest complete snapshot. The first non-checkpoint is retained and processed next, so restore, reconcile, and flush remain strict barriers.
- Restore disables controller/platform commands before reading, applies the persisted snapshot through `PlaybackSessionController`, and saves the normalized `sessionSnapshot()` before collection on normal success. Every non-cancelled restore terminal path then starts exactly one checkpoint collector, marks it ready, reenables commands, and only then exposes `Ready` or `FailedSafe`.
- Reconciliation submitted during restore stays in the actor queue. Normal reconciliation applies controller state, saves its normalized snapshot, and returns `Applied`. Failed-safe reconciliation still applies controller state in memory, skips persistence, and returns `FailedSafeApplied`.
- Unexpected read failure applies the empty paused controller boundary, reenables commands, completes restore, and enters process-lifetime `FailedSafe`.
- Any save failure transitions once to process-lifetime `FailedSafe`; later checkpoints and restores do not persist, while flush/reconcile/restore replies still complete.
- `flush()` awaits collector readiness, then awaits the ordered checkpoint fence before placing its actor barrier.

## Strict TDD evidence

### RED

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result: `BUILD FAILED` during `:shared:compileTestKotlinJvm` because `PlaybackSessionCoordinator`, `PlaybackSessionPhase`, and `PlaybackSessionReconcileResult` were absent. This was the expected meaningful RED before production implementation.

The tests cover:

- command gating and restore order;
- normalized save before checkpoint collection;
- reconciliation queued during restore;
- adjacent-checkpoint-only collapse;
- restore/reconcile/flush barriers;
- flush completion;
- save failure;
- throwing read;
- restore load normalization failure;
- process-lifetime failed-safe future callers;
- one checkpoint collector after repeated restore.

### GREEN

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result: `BUILD SUCCESSFUL in 1s`; 10 focused coordinator tests passed.

## Verification

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`: pass.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 690ms`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- `GIT_MASTER=1 git diff --check`: pass (no output).
- Kotlin LSP is unavailable in this repository; Gradle compilation is the required compiler gate.

## Files

- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinator.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/session/PlaybackSessionCoordinatorTest.kt`
- `.superpowers/sdd/task-5-report.md`

## Commits

- `d2abf52` — `feat: coordinate playback session persistence`
- Durable report commit follows this implementation commit.

## Self-review

- The actor is the sole persistence serialization boundary.
- Checkpoint collapse does not consume across a barrier.
- Restore replies and command reenabling are in a finally-equivalent path.
- Normal restore preserves normalized-save-before-collection order; failed and pre-existing `FailedSafe` restore paths use the same guarded terminal collector startup.
- Persistence is permanently disabled after the first read/save failure; no future waiter depends on actor failure propagation.
- No dependencies, platform engines, lifecycle/DI wiring, store semantics, SQL, UI, OpenSpec, progress, roadmap, or earlier task reports were changed.

## Concerns

- Abrupt process termination may still lose an in-flight write, matching the approved durability limit.
- Task 6 must provide the actual process-owned scope and singleton lifecycle wiring; this task intentionally does not implement it.

## Review fixes

### Root causes

- `flush()` used `yield()` to guess that the checkpoint collector had forwarded prior controller checkpoints. This provided no happens-before relationship between controller checkpoint transport, the collector, and the coordinator actor.
- The actor job was not retained. Actor cancellation or unexpected collector failure could close execution without closing/draining the command channel, so already accepted and future reply-bearing commands could wait forever.
- The checkpoint collector was an unsupervised process-scope child whose exceptional completion did not explicitly terminate the actor or reject future commands.
- Restore treated every throwable as an operational failure but had no direct regression for `controller.restoreSession()` throwing. The empty fallback reused the same method without verifying that a one-time throwing controller could recover safely.

### RED evidence

Command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.checkpointFenceCompletesAfterCollectorConsumesEveryPriorCheckpoint' --configuration-cache
```

Result: `BUILD FAILED` in `:shared:compileTestKotlinJvm` because `PlaybackSessionController.awaitCheckpointFence()` and `PlaybackController.awaitCheckpointFence()` did not exist. The controlled coordinator tests also required the missing deterministic fence and terminal actor contract.

### Fence contract

- The Task 4 checkpoint channel now transports internal ordered envelopes: complete checkpoint snapshots and fence acknowledgements.
- `PlaybackSessionController.awaitCheckpointFence()` inserts a fence into that same unlimited ordered channel and waits for the sole collector to consume every prior envelope.
- `PlaybackSessionCoordinator.flush()` awaits the controller fence before enqueueing its actor `Flush` barrier. No yield, delay, scheduler ordering, or snapshot reread is used.
- Collector startup occurs exactly once in restore's terminal `finally` path for both `Ready` and `FailedSafe`, before restore completion and command reenabling, so failed restore can still fence and flush without hanging.

### Shutdown contract

- The coordinator retains its actor and collector jobs.
- Actor termination closes the command channel, cancels the collector, and drains active, pending, and queued reply-bearing commands with the terminal exception.
- Future enqueue detects the closed channel/dead actor and throws immediately rather than orphaning a reply.
- Process cancellation is propagated as cancellation, not converted to persistence `FailedSafe`.
- Collector exceptional completion records the cause, closes the command channel, and cancels the actor; the actor's terminal drain then rejects current, queued, and future callers.

### GREEN and verification evidence

- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`; controlled fence, 25-repeat race, cancellation drain, collector failure, and throwing restore included).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 668ms`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`).
- `GIT_MASTER=1 git diff --check`: pass (no output).

### Review self-check

- Prior checkpoint emission is deterministically ordered before flush return.
- Flush during `FailedSafe` remains available because restore always starts the sole collector before completing.
- Cancellation cannot be mistaken for read/save failure.
- No accepted reply-bearing command is intentionally left without terminal completion.
- The throwing-controller test verifies first restore throws, empty fallback is attempted once and succeeds, commands reenable, `FailedSafe` persists for process lifetime, future reconcile/flush complete, and later checkpoints never save.
- No platform engine, store, dependency, Task 6 lifecycle/DI/App, SQL, UI, product documentation, OpenSpec, progress, roadmap, or Task 1-4 report changed.

### Review fix commits

- `3767734` — `fix: fence playback checkpoints before flush`
- `be17cc1` — `fix: complete playback coordinator shutdown`
- Durable review evidence commit follows.

## Collector readiness follow-up

### Root cause

The production checkpoint fence correctly rejects calls while its sole collector is inactive. Coordinator `flush()` called that fence immediately, so a flush concurrent with the first restore failed instead of waiting. The pre-existing `FailedSafe` restore branch also returned before terminal collector startup.

### RED evidence

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.session.PlaybackSessionCoordinatorTest' --configuration-cache
```

Result: `BUILD FAILED`; four controlled tests failed. Flush during blocked successful restore, flush during a blocked throwing read, and cancellation while waiting completed too early because the inactive fence threw. Pre-restore reconcile save failure followed by restore also threw `collector inactive` because the early `FailedSafe` branch skipped collector startup.

### Readiness contract

- The coordinator owns one process-lifetime `CompletableDeferred<Unit>` for collector readiness.
- Flush awaits readiness before requesting the ordered controller fence and enqueueing `Flush`.
- Normal successful restore still saves normalized state before collection.
- Every non-cancelled restore terminal path, including throwing read/save/restore and pre-existing `FailedSafe`, starts the same collector exactly once, completes readiness, reenables commands, and completes restore.
- Actor/collector shutdown completes readiness exceptionally, so waiting flush callers cannot hang.
- Cancellation unwind does not start a replacement collector.

### GREEN evidence

- `PlaybackSessionCoordinatorTest`: pass (`BUILD SUCCESSFUL in 387ms`; 19 tests).
- Full `PlaybackControllerTest`: pass (`BUILD SUCCESSFUL in 4s`).
- `LibraryPlaybackSelectionTest`: pass (`BUILD SUCCESSFUL in 717ms`).
- `:shared:compileKotlinJvm`: pass (`BUILD SUCCESSFUL in 5s`).
- `GIT_MASTER=1 git diff --check`: pass.

### Readiness self-review

- Flush cannot invoke the production fence before the coordinator collector is active.
- Flush submitted during restore remains pending and then uses normal fence/barrier ordering.
- Early `FailedSafe` restore no longer bypasses collector startup, command reenabling, or reply completion.
- Collector startup remains single-instance across repeated restore calls.
- Cancellation and collector failure terminate readiness exceptionally and preserve actor draining.

### Readiness fix commits

- `584e73d` — `fix: await playback checkpoint collector readiness`
- Durable readiness evidence commit follows.

---

# Playlist Dialog Polish Task 5 Report

## Status

DONE_WITH_BLOCKERS

### Runtime QA retry — partial desktop semantic/interaction evidence

After the original blocked attempt, Orca Accessibility and Screenshot permissions were available. A new `:desktopApp:run` session exposed a real interactive `RhythHaus` window and ScreenCaptureKit PNG captures at compact 800×600 and wide 1728×1084. Through fresh accessibility snapshots, the controller opened Playlists → Create Playlist, confirmed title `创建播放列表`, text field `播放列表名称`, and actions `取消` / `创建播放列表`, then used the real Cancel action to return to the playlist hub. A separate wide-window snapshot re-opened the same dialog.

This is evidence for desktop semantics and basic pointer interaction only. The PNGs could not be rendered by the available review tooling, so panel opacity, scrim, colors, spacing, typography, clipping/CJK metrics, and visual containment remain unverified. Escape later returned to the enclosing Library route, so it is not evidence of dialog-specific keyboard dismissal. Dark theme, other migrated dialog families, and Android/iOS runtime UI were not exercised. OpenSpec 4.2 stays open.

Task 5 ran every required verification command and attempted desktop runtime QA. Product source and tests were not modified. OpenSpec 1.2 remains open because the existing common test does not assert the accessible dismiss action. OpenSpec 4.2 remains open because no usable runtime visual or interaction state could be observed.

## Required Verification

### Strict OpenSpec

Command:

```bash
openspec validate playlist-dialog-polish --strict
```

Exact output:

```text
Change 'playlist-dialog-polish' is valid
```

### JVM, Desktop, and Android

Command:

```bash
./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache
```

Exact successful ending:

```text
BUILD SUCCESSFUL in 4m 21s
110 actionable tasks: 18 executed, 2 from cache, 90 up-to-date
Configuration cache entry stored.
```

The run emitted the existing Android deprecation warning for `MediaMetadata.Builder.setArtworkData` at `PlaybackEngine.android.kt:474:17`.

### Xcode

Command:

```bash
/usr/bin/xcrun xcodebuild -version
```

Exact output:

```text
Xcode 26.6
Build version 17F113
```

### iOS Simulator Tests

Command:

```bash
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
```

Exact successful ending:

```text
BUILD SUCCESSFUL in 31s
44 actionable tasks: 9 executed, 35 up-to-date
Configuration cache entry stored.
```

This proves the automated iOS simulator test suite passed. It does not prove any iOS runtime UI, touch, or visual state.

### Diff Hygiene

Initial command:

```bash
GIT_MASTER=1 git diff --check
```

Result: pass with no output. The post-evidence rerun is recorded under Final Checks.

## Desktop Runtime Attempt

Launch command:

```bash
./gradlew :desktopApp:run --configuration-cache
```

Observed evidence:

- The launch log reached `> Task :desktopApp:run`.
- The application process was PID 40200 with main class `com.eterocell.rhythhaus.MainKt`.
- `orca computer list-apps --json` listed the running process as `MainKt`.
- `orca computer list-windows --app pid:40200 --json` listed one visible, non-minimized `RhythHaus` window at x=56, y=87 with width 784 and height 588.
- `orca computer capabilities --json` reported screenshot, element-frame, click, scroll, drag, keyboard, and value actions as supported.
- `orca computer permissions --json` reported Accessibility and Screenshots as `granted`.

Blocked evidence:

- `orca computer get-app-state --app pid:40200 --restore-window --json` failed with `permission_denied`: the app had visible windows but no accessibility window.
- Retrying with window ID 24836 failed with the same `permission_denied` result.
- `orca computer press-key --app pid:40200 --window-id 24836 --key Tab --json` failed with the same result, so keyboard focus/submit was not exercised.
- Native capture of window 24836 failed with `could not create image from window`.
- Full-screen capture succeeded as a file but contained no usable desktop pixels; image inspection could not observe the app.
- The launched Gradle and application processes were terminated after the attempt.

No screenshot-based dual visual review was run because no usable capture existed. Running those passes against a black image would fabricate visual evidence.

## Explicit Manual Gaps

None of these states is claimed as passed:

- compact or wide rendered playlist layouts;
- light or dark rendered themes;
- Saved/Queue text fit or contrast;
- Clear Library, Remove Folder, create/rename, Add to Playlist, track browser, saved destructive confirmation, or Clear Upcoming dialogs;
- solid panel opacity or dark-theme light scrim brightness;
- long localized/CJK labels, Latin descenders, clipping, wrapping, or body scrolling;
- keyboard focus, tab order, submit, dismiss, or validation-state retention;
- scrim tap dismissal, panel touch containment, Android/iOS touch behavior;
- Now Playing bottom clearance or overlap;
- Android or iOS runtime UI and visual behavior.

## Read-only Review

Two independent read-only lanes ran after verification:

1. Evidence-discipline review: `REVISE`. It approved the strict/platform evidence but required all unavailable runtime states to remain gaps and recommended leaving OpenSpec 4.2 open.
2. Whole-change source review: `REVISE`. It identified the missing accessibility-dismiss assertion in `HausDialogTest.kt`; OpenSpec 1.2 remains open. It also proposed an infinite-height nested-scroll failure for `PlaylistTrackBrowser`, but CodeGraph showed the inner `LazyColumn` is explicitly bounded by `height(320.dp)`. That proposed failure was not accepted as established.

The reviews do not establish visual acceptance. Source-level evidence supports the solid panel/scrim policy, dialog migrations, playlist inset/palette/metric policies, and their focused compilation/tests.

## OpenSpec Disposition

Marked complete from source/tests and fresh verification: 1.1, 2.1, 2.2, 3.1, 3.2, and 4.1.

Left open:

- 1.2: presentation colors and bounds are asserted, but the accessible dismiss action is not.
- 4.2: runtime launched, but compact/wide, light/dark, dialog, text, keyboard, panel/scrim, and Now Playing visual states were not observable.

## Scope

Only these evidence artifacts were modified:

- `openspec/changes/playlist-dialog-polish/tasks.md`
- `progress.md`
- `roadmap.md`
- `.superpowers/sdd/task-5-report.md`

No commit was created, as required.

## Final Checks

- Post-evidence `openspec validate playlist-dialog-polish --strict`: pass; exact output `Change 'playlist-dialog-polish' is valid`.
- Post-evidence `GIT_MASTER=1 git diff --check`: pass with no output.
- Scoped diff/status review confirmed Task 5 changed only the four authorized evidence artifacts. The Task 1-4 product/test working tree remains present and was not modified by Task 5.
- `lsp_diagnostics` was attempted for all four changed Markdown files; no `.md` LSP server is configured, so no diagnostics-clean claim is made.

Route: openspec+superpowers / Task 5 evidence closure with blockers
Next owner: implementation for the focused accessibility-dismiss contract test, then user/manual QA in an attachable runtime
Blockers: missing accessible-dismiss test evidence and unavailable runtime visual/interaction access
