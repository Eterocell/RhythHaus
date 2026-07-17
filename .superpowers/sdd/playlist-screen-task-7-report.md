# Playlist Screen Task 7 Report

Status: DONE_WITH_CONCERNS

Route: openspec+superpowers / systematic-debugging / TDD / Task 7 integration and acceptance
Owner: implementation
Scope: OpenSpec `playlist-screen` tasks 7.1-7.4

## Outcome

- Task 7.1 automated integration proof is complete: real SQLDelight source removal and clear-library cascades complete before reconciliation and playlist publication, saved edits do not retroactively mutate an active resolved queue, and authoritative track deletion reconciles only unavailable occurrences.
- Task 7.2 automated verification is complete, including actual successful `LibraryDatabaseIosTest` execution through the production iOS factory.
- Task 7.3 is complete with concerns based on actual compact Chinese Saved/Queue runtime interaction. Wide, light/dark, English, complete Saved CRUD/add/duplicate/drag flows, pixel-level Now Playing overlap, and target-device audible playback remain explicitly unverified follow-up evidence and are not claimed as passes.
- Task 7.4 remains incomplete pending the controller's final whole-branch review. OpenSpec 7.1-7.3 are complete; 7.4 remains unchecked until that review passes.

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
- OpenSpec task 7.4 remains unchecked until the controller completes final whole-branch review.
- Generic Task 1-7 scratch reports are preserved; change-specific copies are the durable evidence paths.
- Unverified wide/light/dark/English/pixel/device/audio items remain follow-up evidence, not passes and not accepted risk.
- Next safe action: controller final whole-branch review, then complete 7.4 and its final evidence commit if clean. Separately collect the unverified manual/device evidence when a controllable runtime/device and pixel-capable reviewer are available.

## Final task-scoped review

An independent read-only reviewer inspected the exact Task 7 test and report changes and reran:

```text
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaylistLifecycleIntegrationJvmTest' --tests 'com.eterocell.rhythhaus.AppDispatcherJvmTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache
```

Verdict: PASS. No Critical or Important defects, invalid test claims, scope creep, or false completion evidence were found. The review predates Oracle adjudication and confirmed the real shared-database integration coverage, minimal JVM-only test relocation, successful iOS FK result evidence, and honest recording of every unverified manual/device limitation. Oracle subsequently classified those limitations as follow-up evidence rather than blockers.
