# Playlist Screen Task 7 Report

Status: COMPLETE_WITH_INCONCLUSIVE_MANUAL_SUBCASES

Route: openspec+superpowers / systematic-debugging / TDD / Task 7 integration and acceptance
Owner: implementation
Scope: OpenSpec `playlist-screen` tasks 7.1-7.4

## Outcome

- Task 7.1 automated integration proof is complete: real SQLDelight source removal and clear-library cascades complete before reconciliation and playlist publication, saved edits do not retroactively mutate an active resolved queue, and authoritative track deletion reconciles only unavailable occurrences.
- Task 7.2 automated verification is complete, including actual successful `LibraryDatabaseIosTest` execution through the production iOS factory.
- Task 7.3 is complete with concerns based on actual compact Chinese Saved/Queue runtime interaction. Wide, light/dark, English, complete Saved CRUD/add/duplicate/drag flows, pixel-level Now Playing overlap, and target-device audible playback remain explicitly unverified follow-up evidence and are not claimed as passes.
- Task 7.4 is complete after the controller's final corrected whole-branch review passed all five lanes. OpenSpec is 29/29 complete.

## Task 7.1 integration evidence

Added `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/PlaylistLifecycleIntegrationJvmTest.kt` with two real-database tests:

- `sourceRemovalCascadesBeforePlaylistPublicationAndReconcilesResolvedQueue`
- `clearLibraryCascadesBeforePlaylistPublicationAndReconcilesResolvedQueue`

The tests share one production `LibraryDatabase` between `SqlDelightLibraryRepository` and `SqlDelightPlaylistRepository`, use the real App lifecycle helpers and a real `PlaybackController`, and assert:

- playlist-entry cascades are already visible when reconciliation begins;
- lifecycle order is `reconcile -> read_playlists -> library -> playlists`;
- affected playlists survive with empty entry lists;
- a saved remove/reorder after queue resolution leaves the active queue unchanged;
- source removal keeps a surviving active occurrence even when its saved entry was edited away;
- clear-library reconciliation empties the resolved queue after all tracks become unavailable.

Focused command:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaylistLifecycleIntegrationJvmTest' --configuration-cache
BUILD SUCCESSFUL in 2s
```

Existing lifecycle suite:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache
BUILD SUCCESSFUL in 721ms
```

## iOS blocker investigation and FK proof

Exact reproduction before the fix:

```text
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
e: AppScanCancellationTest.kt:64:28 Unresolved reference 'Thread'.
e: AppScanCancellationTest.kt:340:27 Unresolved reference 'Thread'.
BUILD FAILED in 10s
```

Root cause: `clearLibraryRunsRepositoryWorkOnProvidedDispatcher` and its `ThreadCapturingRepository` fixture were JVM thread-affinity checks incorrectly located in `commonTest`. Kotlin/Native has no common `Thread` API; all other tests in that file were portable.

Minimal compatibility fix:

- moved only the JVM dispatcher test and fixture to `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/AppDispatcherJvmTest.kt`;
- removed their JVM-only imports and declarations from `AppScanCancellationTest.kt`;
- changed no production code or test behavior.

Moved/portable test verification:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppDispatcherJvmTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache
BUILD SUCCESSFUL in 5s
```

Full native verification:

```text
./gradlew :shared:iosSimulatorArm64Test --configuration-cache
BUILD SUCCESSFUL in 3m 28s
35 actionable tasks: 7 executed, 28 up-to-date
```

Actual test result evidence:

```text
shared/build/test-results/iosSimulatorArm64Test/TEST-iosSimulatorArm64Test.com.eterocell.rhythhaus.library.LibraryDatabaseIosTest.xml
tests="1" skipped="0" failures="0" errors="0"
productionIosFactoryRejectsInvalidPlaylistEntryForeignKeys[iosSimulatorArm64]
```

The expected invalid insert emitted `SQLITE_CONSTRAINT` inside the passing assertion. `[blocked] iOS FK proof` is resolved; no iOS risk acceptance is needed.

## Required verification matrix

- `openspec validate playlist-screen --strict` — PASS: `Change 'playlist-screen' is valid`.
- `./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration --configuration-cache` — PASS, `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` — PASS, `BUILD SUCCESSFUL in 5s`; 116 actionable tasks, 11 executed, 105 up-to-date.
- `/usr/bin/xcrun xcodebuild -version` — PASS: Xcode 26.6, build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` — PASS with actual native execution as recorded above; subsequent same-session confirmation was up-to-date and successful.
- `GIT_MASTER=1 git diff --check` — PASS with no output.
- Kotlin LSP diagnostics — unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compiled all changed source sets.

## Manual/runtime QA actually verified

Runtime:

- launched the worktree with `./gradlew :desktopApp:run --configuration-cache`;
- inspected the real `RhythHaus` window through Orca at 800x600 with Accessibility and screenshot permissions granted;
- runtime locale was Chinese.

Verified Saved state:

- opened the Playlist hub through the live `打开播放列表` action;
- observed localized `已保存` and `队列` tabs, `创建播放列表`, and the empty Saved state `还没有已保存的播放列表`;
- opened the localized create dialog and dismissed it with Escape; keyboard delivery was reported as synthetic/unverified, but the dialog visibly disappeared from the refreshed accessibility tree.

Verified Queue state and interaction:

- current occurrence rendered first under `当前曲目` and had no drag/move/remove descendants;
- upcoming rows had track-named localized reorder, move-up, move-down, and remove controls;
- first upcoming move-up and final upcoming move-down boundaries were disabled;
- invoked the first upcoming row's accessible move-down action and observed the ordered row swap while current remained pinned;
- invoked immediate remove on the moved row and observed only that row disappear while current remained pinned;
- opened `清空接下来曲目` and observed the localized confirmation `要清空所有接下来曲目吗？当前曲目会继续播放。`; did not confirm destructive clearing;
- screenshot capture succeeded for each state, but this model cannot decode image pixels.

## Unverified follow-up evidence

The available Orca provider reports `moveResize=false`, `focus=false`, and no OCR. It could not reliably resize the Compose window or provide verified keyboard focus, and this model cannot inspect the captured PNG pixels. The live environment also exposed only the current Chinese/system-theme state and no target mobile device/audio path.

Not verified and therefore not claimed:

- wide layout runtime behavior;
- separate light and dark visual states;
- English runtime localization;
- Saved create/rename/delete success workflows;
- duplicate saved rows and both add paths;
- saved drag gesture and accessible moves on populated data;
- dialog keyboard focus/submit behavior;
- queue drag gesture, clear confirmation execution, and stale-command rejection UI;
- pixel-level Now Playing overlap, spacing, contrast, or CJK glyph fidelity;
- target-device audible playback.

Automated tests cover these behaviors at model/controller/source level, but they do not satisfy the explicit real manual/device acceptance requirement.

## Completion discipline

- OpenSpec tasks 7.1-7.3 are complete under Oracle adjudication because the explicit iOS FK hard gate executed successfully and actual runtime results are recorded with limitations.
- OpenSpec task 7.4 is complete after the controller's five-lane final whole-branch review passed.
- Generic Task 1-7 scratch reports are preserved; change-specific copies are the durable evidence paths.
- Unverified wide/light/dark/English/pixel/device/audio items remain follow-up evidence, not passes and not accepted risk.
- Next safe action: archive only when explicitly requested. Separately collect the unverified manual/device evidence when a controllable runtime/device and pixel-capable reviewer are available.

## Final task-scoped review

An independent read-only reviewer inspected the exact Task 7 test and report changes and reran:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaylistLifecycleIntegrationJvmTest' --tests 'com.eterocell.rhythhaus.AppDispatcherJvmTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache
```

Verdict: PASS. No Critical or Important defects, invalid test claims, scope creep, or false completion evidence were found. The review predates Oracle adjudication and confirmed the real shared-database integration coverage, minimal JVM-only test relocation, successful iOS FK result evidence, and honest recording of every unverified manual/device limitation. Oracle subsequently classified those limitations as follow-up evidence rather than blockers.

## Post-commit verification

- `openspec validate playlist-screen --strict`: PASS.
- `./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration --configuration-cache`: PASS.
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: PASS (`BUILD SUCCESSFUL in 1s`; 116 actionable tasks, 4 executed, 1 from cache, 111 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: PASS (Xcode 26.6, build 17F113).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache --rerun-tasks`: PASS (`BUILD SUCCESSFUL in 33s`; all 35 tasks executed).
- Fresh native XML timestamp `2026-07-17T10:19:56.156Z` records `LibraryDatabaseIosTest` tests=1, skipped=0, failures=0, errors=0 and expected `SQLITE_CONSTRAINT` output.
- `GIT_MASTER=1 git diff --check`: PASS.

## Final evidence/context correction

- Restored generic `.superpowers/sdd/task-2-report.md` exactly to its pre-playlist `fd95340` unrelated artwork report. Playlist Task 2 evidence remains preserved only at `.superpowers/sdd/playlist-screen-task-2-report.md`.
- Corrected OpenSpec Task 6.3, approved plan wording, and durable Task 6 evidence to match the reviewed role-free implementation: queue entry containers use accurate localized content/state descriptions and do not receive a false semantic role when Compose lacks a suitable list-row role; artwork retains separate image semantics.
- No production or test code changed. OpenSpec 7.4 is complete based on the controller's final review.
- Wide, light/dark, English, populated Saved workflows, drag gestures, pixel/CJK fidelity, and target-device audible playback remain unverified follow-up evidence and are not claimed as passes.

## Controller final whole-branch review

The controller reviewed the final corrected whole-branch package through `6f2d519`. All five terminal lanes passed after the generic Task 2 restoration and role-wording correction:

| Lane | Verdict | Confidence / maximum severity | Notes |
| --- | --- | --- | --- |
| Goal | PASS | High confidence | The implementation and durable evidence satisfy the approved playlist-screen change. |
| QA | PASS | High confidence | Automated and observed runtime evidence is sufficient for the release gate; the runtime subcases listed below remain INCONCLUSIVE rather than passes. |
| Code Quality | PASS | High confidence | No blocking maintainability or correctness finding. |
| Security | PASS | Maximum LOW | Non-blocking follow-ups: align repository name/list/count limits with the persistence codec, and bound or coalesce the checkpoint queue. No hardening was added in this evidence-only closure. |
| Context | PASS | High confidence | OpenSpec, approved plan, implementation, tests, and durable reports are aligned. |

There are no Critical, Important, or Medium findings. The following runtime/manual subcases remain INCONCLUSIVE and unverified: wide layout; separate light/dark states; English runtime localization; populated Saved create/rename/delete; duplicate saved rows; both add paths; saved and queue drag gestures; verified keyboard focus/submit; clear execution and stale-command rejection; pixel-level Now Playing overlap/spacing/contrast/CJK fidelity; target-device audible playback. They are not claimed as passes.

Task 7.4 is complete: the controller's five-lane final review passed, the generic Task 2 report remains restored to the unrelated pre-playlist evidence, and the queue accessibility contract remains corrected to accurate role-free content/state semantics with separate artwork image semantics.
