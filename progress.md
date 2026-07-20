    # Session Progress

## Archive follow-up - 2026-07-20 playlist-edit-mode-bottom-bar-dialog-theme

Route: openspec+superpowers / spec sync and archive
Owner: OpenSpec lifecycle complete; next owner is user/manual runtime QA
Input: completed and validated `playlist-edit-mode-bottom-bar-dialog-theme` change at lifecycle commit `3aafd7f`.
Output: synced the complete delta requirements into main capabilities `playlist-detail-editing`, `playlist-scroll-chrome`, and `theme-aware-dialog-exterior`; archived the intact change at `openspec/changes/archive/2026-07-20-playlist-edit-mode-bottom-bar-dialog-theme`.
Verification: strict active-change validation passed before the move; all three synced main specs pass strict validation; post-move `openspec list --json` no longer lists the change; `git diff --check` passed. The CLI cannot validate a dated archive as a change because archived directories are outside its active-change discovery, so no post-move archived-change validation is claimed.
Next owner: user/manual runtime QA for the already-recorded visual, physical gesture, screen-reader, and target-device gaps.
Blockers: none; archive was explicitly requested and no push was performed.

## Handoff - 2026-07-20 playlist-edit-mode-bottom-bar-dialog-theme completion

Route: openspec+superpowers / final lifecycle and evidence handoff
Owner: harness verification complete; next owner is user/manual runtime QA or OpenSpec archival on explicit request
Input: approved `playlist-edit-mode-bottom-bar-dialog-theme` change, implementation commits `3ced48b`, Task 2 sequence through `ee61662`, Task 3 `dae162a` plus `59ad4c9`, `deb2164`, and `58b567d`, Task 4 `65a5b52`, Task 5 `4b42e7d`, and final independent Oracle APPROVE after `58b567d`.
Output:
- Completed the OpenSpec checklist without archiving the change.
- Playlist detail is playback-first by default and supports page-wide long-press edit mode with exact occurrence-ID controls; shell Back precedence is modal -> edit -> selection -> Now Playing -> direct route pop.
- Playlist detail reports its real lazy-list position into the shared Bottom Bar scroll policy and uses measured shell clearance; both dialog variants use the shared theme-aware HausDialog exterior policy.
Verification:
- `openspec validate playlist-edit-mode-bottom-bar-dialog-theme --strict`: valid.
- Exact six-class focused matrix from OpenSpec 6.2 with `--rerun-tasks`: pass (`BUILD SUCCESSFUL in 10s`; 26 actionable tasks executed).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 11s`; 101 actionable tasks, 12 executed and 89 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 39s`; 44 actionable tasks, 21 executed and 23 up-to-date).
- `git diff --check`: pass; final independent Oracle review: APPROVE.
Manual gaps: compact/wide pixel appearance, physical long-press/drag, Bottom Bar animation feel, dark/light rendered dialog exterior, screen-reader behavior, and target-device behavior were not performed and remain unverified follow-up evidence, not automated-completion blockers.
Changed lifecycle files: `openspec/changes/playlist-edit-mode-bottom-bar-dialog-theme/tasks.md`, `progress.md`, `roadmap.md`, and dedicated Task 6 evidence under `.superpowers/sdd/`; existing Task 1 evidence remains `.superpowers/sdd/task-1-report.md` from `3ced48b`.
Next owner: user/manual runtime QA; OpenSpec archival only when explicitly requested.
Blockers: none for automated completion; manual runtime/visual/accessibility evidence remains outstanding.
Commits: `3ced48b`, Task 2 sequence ending `ee61662`, `dae162a`, `59ad4c9`, `deb2164`, `58b567d`, `65a5b52`, and `4b42e7d`; lifecycle commit follows this handoff.

## Handoff - 2026-07-19 track-multi-select-playlist-backup Task 9

Route: openspec+superpowers / integration, supported-platform verification, runtime/visual QA attempt, and final review
Owner: automated Task 9 review findings complete; next owner is user/manual runtime and visual QA
Input: `.superpowers/sdd/task-9-brief.md`, accepted Tasks 1-8 through `4462380`, and approved OpenSpec change
Output:
- Added and committed real temporary-database integration acceptance as `0f48f45 test: verify playlist backup integration`.
- Final review exposed iOS short-read oversized-detection risk; strict native RED/GREEN fixed bounded accumulation and committed `912e6e4 fix: bound iOS playlist backup reads`.
- The bounded-read defect remains fixed. Commit `5741a4d5233ac1f5b0ec3a70aa7a259153bb5cbc` adds 80 exhaustive local-only canary assertions across every exported entry; `6af0ba85e629819937fcee60dad6d9909234ee45` records exact focused XML counts and passed post-commit diff hygiene.
- Controller-visible independent re-review at evidence HEAD `6af0ba85e629819937fcee60dad6d9909234ee45` returned Spec Compliance PASS and Task Quality PASS with no Critical, Important, or Minor findings. It confirmed privacy coverage, positive portable metadata, exact counts, lifecycle alignment, runtime limitations, and test/documentation-only scope.
Verification:
- Exact forced Task 1-8 JVM matrix command is recorded in `.superpowers/sdd/track-multi-select-playlist-backup-task-9-report.md`; fresh XML: 20 selected suites, 272 tests, 0 failures, 0 errors, 0 skipped.
- Exact focused Android-host adapter command: `./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsAndroidTest' --configuration-cache --rerun-tasks`; fresh XML: 10 tests, 0 failures, 0 errors, 0 skipped.
- Controlled privacy-helper RED: exact focused command ran 1 test, failed 1 on injected `private-source-id-alpha`, skipped 0; the temporary leak test was removed. Unchanged-production integration GREEN: 1 test, 0 failures/errors/skips. No production source changed.
- `openspec validate track-multi-select-playlist-backup --strict`: pass.
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s`; 125 tasks) before final fix; post-fix pass (`BUILD SUCCESSFUL in 517ms`; 116 tasks).
- Xcode 26.6 build 17F113; full `:shared:iosSimulatorArm64Test` pass (`BUILD SUCCESSFUL in 36s`) and post-fix pass.
- Native XCTest: 7/7 initially; review-fix RED failed exactly because short reads did not throw; GREEN and complete native suite passed 8/8.
- Unsigned clean generic-simulator Xcode build: pass before and after fix. `GIT_MASTER=1 git diff --check`: pass before evidence editing.
Runtime/visual QA:
- Desktop launched PID 74386 with visible 784x588 window; Orca permissions were granted but no AX window was exposed, native window capture failed, and the full-screen PNG could not be rendered.
- iPhone 17 Pro simulator app launched PID 75202 and framebuffer PNG was captured; Simulator exposed no AX window and the PNG could not be rendered.
- `adb devices -l`: no Android device/emulator.
- Unverified, not passes: desktop/mobile long press, tap/touch, checkbox/accessibility actions, navigation/Search cleanup, picker and Settings system panels, actual import/export paths, compact/wide, light/dark, English/Chinese/CJK, focus, row fit, contextual-bar clearance/mutual exclusion, and preview late-row pixels.
Acceptance:
- OpenSpec 8.1-8.3 and 9.4 are complete. OpenSpec 9.1-9.3 remain open for unavailable runtime/system-panel/visual acceptance.
- No archive or push. Generic `.superpowers/sdd/task-1-report.md` and `task-2-report.md` remain preserved and must not be staged.
- Immediate controller post-commit gate for the dedicated Task 9.4 closure: run `GIT_MASTER=1 git diff --check`, `GIT_MASTER=1 git status --short`, and `openspec validate track-multi-select-playlist-backup --strict` at the new HEAD; no result for that not-yet-created HEAD is claimed in this tracked handoff.
Changed lifecycle files:
- `.superpowers/sdd/track-multi-select-playlist-backup-task-9-report.md`
- `.superpowers/sdd/progress.md`
- `openspec/changes/track-multi-select-playlist-backup/tasks.md`
- `progress.md`
- `roadmap.md`
Next safe action: perform Task 9.1-9.3 manually with attachable accessibility/device tooling and renderable screenshots; archive only after explicit request.
Blockers: runtime/system-panel and visual acceptance only; no automated privacy/evidence/review/OpenSpec blocker.

## Handoff - 2026-07-19 playlist-dialog-polish Task 5 verification

Route: openspec+superpowers / Task 5 verification, runtime-QA attempt, and final read-only review
Owner: verification complete with blockers; next owner is implementation for the missing accessibility-contract test, then user/manual QA
Input: `.superpowers/sdd/task-5-brief.md` and the review-clean Task 1-4 working tree.
Output:
- Verified the strict OpenSpec and supported JVM, desktop, Android, Xcode, and iOS simulator gates without modifying product source, tests, dependencies, routes, strings, palette, persistence, or playback.
- Launched the real desktop app through `:desktopApp:run`; Orca listed a visible `RhythHaus` window at 784x588, but could not attach an accessibility window or deliver keyboard input. No usable screenshot was available.
- Marked only source/policy/migration and platform-verification OpenSpec items complete. Items 1.2 and 4.2 remain open.
Verification:
- `openspec validate playlist-dialog-polish --strict`: pass; exact output `Change 'playlist-dialog-polish' is valid`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; `BUILD SUCCESSFUL in 4m 21s`; `110 actionable tasks: 18 executed, 2 from cache, 90 up-to-date`; configuration cache stored. The existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning remained.
- `/usr/bin/xcrun xcodebuild -version`: pass; `Xcode 26.6`; `Build version 17F113`.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass; `BUILD SUCCESSFUL in 31s`; `44 actionable tasks: 9 executed, 35 up-to-date`; configuration cache stored. This is an automated iOS simulator test pass, not iOS runtime UI or visual QA.
- Initial `GIT_MASTER=1 git diff --check`: pass with no output. A final post-evidence diff check is recorded in `.superpowers/sdd/task-5-report.md`.
Runtime QA:
- `./gradlew :desktopApp:run --configuration-cache` reached `:desktopApp:run`; process PID 40200 and the 784x588 `RhythHaus` window were observed, then terminated after the QA attempt.
- `orca computer capabilities --json` reported screenshot, element-frame, keyboard, click, scroll, and drag support. `orca computer permissions --json` reported Accessibility and Screenshots as `granted`.
- `get-app-state`, the explicit window-ID retry, and a `Tab` action each failed with `permission_denied`: the visible app had no accessibility window. Native window capture failed; the full-screen capture was black/unusable.
- Unverified, not passes: compact and wide rendered layouts; light and dark themes; Saved/Queue text fit and contrast; every migrated dialog family; solid panel opacity; dark-theme light scrim brightness; long localized/CJK text and descenders; keyboard focus/submit; panel/scrim pointer behavior; Now Playing clearance; Android/iOS runtime UI, touch, or visual behavior.
Review:
- Evidence-discipline review: `REVISE`; it approved completion of source/policy/migration and platform gates but required all runtime states to remain explicit gaps.
- Whole-change source review: `REVISE`; its substantiated Important finding is that `HausDialogTest.kt` has no assertion for the specified accessibility dismiss action. OpenSpec 1.2 remains open.
- The same review proposed an infinite-height nested-scroll failure for `PlaylistTrackBrowser`; final adjudication did not accept that claim because the nested `LazyColumn` has an explicit `height(320.dp)` bound. No runtime pass is inferred from this source adjudication.
Changed evidence files:
- `openspec/changes/playlist-dialog-polish/tasks.md`
- `progress.md`
- `roadmap.md`
- `.superpowers/sdd/task-5-report.md`
Next safe action: add a focused accessibility-dismiss contract test without changing dialog behavior, rerun the focused and full gates, then manually exercise the listed compact/wide, theme, dialog, text, keyboard, scrim/panel, and Now Playing states in an attachable runtime.
Blockers: missing accessible-dismiss test evidence and unavailable runtime visual/interaction access. No strict OpenSpec, JVM, desktop compile, Android assemble, Xcode, iOS simulator test, or initial diff-hygiene blocker.
Commit: skipped; the user explicitly prohibited commits.

## Follow-up - 2026-07-19 playlist-dialog-polish accessibility contract

Route: openspec+superpowers / Task 6 JVM accessibility semantics regression
Owner: implementation complete; runtime visual QA remains user/manual
Input: Task 5 source review finding that `HausDialogTest` did not exercise the accessible dismiss action.
Output:
- Added a JVM-only Compose UI test using the current `androidx.compose.ui.test.v2.runComposeUiTest` API. It finds `SemanticsActions.Dismiss`, invokes it, proves the real callback runs exactly once, and proves state removes the dialog semantics node.
- Added only `org.jetbrains.compose.ui:ui-test:1.11.1` and `compose.desktop.currentOs` under `jvmTest.dependencies`; production and Android/iOS source sets are unchanged.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.HausDialogSemanticsJvmTest' --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 8s`; 26/26 tasks executed).
- `openspec validate playlist-dialog-polish --strict`: pass.
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL in 1m 21s`).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 9s`).
- `/usr/bin/xcrun xcodebuild -version`: pass (Xcode 26.6, build 17F113).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- `GIT_MASTER=1 git diff --check`: pass.
Acceptance:
- OpenSpec 1.2 and 5.1 are complete. OpenSpec 4.2 remains [blocked] because the launched desktop app exposed no accessible window and no usable capture; compact/wide, light/dark, dialog, CJK/text-fit, keyboard, panel/scrim, and Now Playing runtime QA remain manual only.
Next owner: user/manual QA in an attachable desktop, Android, or iOS runtime; then OpenSpec archival on explicit request.
Blockers: runtime visual/interaction capture only.
Commit: skipped; user did not request one.

## Final verification - 2026-07-19 playlist-dialog-polish

Route: openspec+superpowers / final source review and supported matrix
Owner: implementation complete; runtime visual QA remains user/manual
Output:
- Final source review initially found that Remove Folder lost its pre-existing localized Cancel dismiss-action label. The shared `HausDialog` now accepts an optional label, Remove Folder passes its existing localized resource, and the JVM semantics test proves that label, exactly one real callback, and semantics-node removal.
- Final reviewer re-review: PASS with no Critical or Important findings. Oracle review: PASS with no Critical or Important source-level finding; both retain runtime visual QA as an evidence gap rather than a pass.
Verification:
- `openspec validate playlist-dialog-polish --strict`: pass.
- `./gradlew :shared:jvmTest --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 1m 26s`; 26/26 tasks executed).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 12s`).
- `/usr/bin/xcrun xcodebuild -version`: pass (Xcode 26.6, build 17F113).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 28s`).
- `GIT_MASTER=1 git diff --check`: pass.
- One earlier full JVM run failed only at unchanged `PlaybackControllerTest.repeatPlaylistWrapsCompletionAndManualTransport`; three forced isolated reproductions and the final forced full JVM run passed. CodeGraph found no call path from the dialog/UI change, so no unrelated playback change was made.
Acceptance:
- OpenSpec implementation and automated evidence are complete. OpenSpec 4.2 remains [blocked]: the desktop app window launched but Orca could not attach to its accessibility window or produce usable capture, so rendered compact/wide, light/dark, dialogs, text/CJK, keyboard, panel/scrim, and Now Playing QA remain manual only.
Next owner: user/manual QA in an attachable desktop, Android, or iOS runtime; archive only on explicit request.
Blockers: runtime visual/interaction capture only.
Commit: skipped; user did not request one.

## Follow-up - 2026-07-19 playlist-dialog-polish partial desktop runtime QA

Route: openspec+superpowers / runtime accessibility and capture retry
Owner: verification complete; visual acceptance remains manual/tooling-blocked
Input: previously blocked OpenSpec 4.2 runtime QA for shared dialogs and playlist controls
Output:
- Orca now reports Accessibility and Screenshot permissions as granted. `:desktopApp:run` produced a real interactive `RhythHaus` window and captured PNGs through ScreenCaptureKit.
- At compact 800×600 and wide 1728×1084 desktop windows, real accessibility navigation opened Playlists and Create Playlist. The live dialog exposed title `创建播放列表`, field `播放列表名称`, and actions `取消` / `创建播放列表`; activating Cancel returned to the playlist hub. This proves the UI is live interactive Compose, not a static image, for that desktop route.
- The wide-state dialog was independently re-opened from a fresh accessibility snapshot. Escape later exited the enclosing playlist route, so it is not treated as proof of dialog-specific keyboard dismissal.
- Captures exist, but neither available image-review path could render pixels. No claim is made about panel opacity, scrim, color/contrast, spacing/alignment, typography, CJK glyph/descender fit, or pointer containment from those images.
Acceptance:
- OpenSpec 4.2 remains open. The current evidence supersedes the obsolete “no accessible desktop window/capture” claim with partial desktop semantic/interaction coverage only.
- Still required for 4.2: renderable compact/wide screenshots in light and dark themes; visual review of all migrated dialogs and playlist controls; dialog-specific keyboard behavior if required; Android/iOS runtime UI inspection.
Next owner: user/manual QA or a session with a renderable screenshot-review path; archive only on explicit request after remaining 4.2 evidence is accepted.
Blockers: pixel rendering/review unavailable to the agent; dark-theme, other-dialog-family, Android, and iOS runtime coverage not run.
Commit: skipped; user did not request one.

## Fix - 2026-07-17 desktop completed scans deleted their own persisted rows

Route: systematic-debugging + strict RED/GREEN TDD + live desktop reproduction
Owner: implementation
Input: User reported the desktop scan remained broken after the JVM legacy-database compatibility repair.
Root cause:
- A controlled desktop run selected and scanned the real `Zemeth` folder. The UI reported `扫描完成：新增 92 首，更新 0 首`, but a simultaneous read-only query of the same process database contained one source and zero tracks, scan sessions, and scan errors.
- `LibraryScanner.scan()` correctly writes the source/session, every track/error, then at normal completion writes `source.copy(lastScanAtEpochMillis = completedAt)` before its terminal session update.
- `LibrarySource.sq` implemented `upsertSource` as `INSERT OR REPLACE`. SQLite REPLACE deletes the existing parent source before reinsertion; enabled foreign keys cascade-deleted `library_track` and `scan_session`, then `scan_error`. The final session update therefore matched nothing.
Fix:
- Replaced the destructive source REPLACE with one SQLDelight atomic `upsertSource` block: `INSERT OR IGNORE` plus full-field `UPDATE WHERE id = :id`. This preserves in-place source metadata semantics while never deleting the parent and its children.
- An initial `ON CONFLICT DO UPDATE` form was rejected in final review because Android minSdk 29 uses SQLite 3.22, which predates that UPSERT grammar. The final portable compound form uses only API-29-compatible syntax.
- Retained the separate JVM legacy `user_version = 0` compatibility bootstrap and its regression coverage because it fixes a distinct startup/open failure exposed by the same bisect investigation.
TDD and live evidence:
- RED: `completedScanTerminalSourceUpdatePreservesPersistedChildren` uses a real JDBC `LibraryDatabase`/repository/scanner and emits one candidate plus one skipped file. Before the query repair it observed a completed source timestamp but `[]` tracks, no session, and no scan error—matching the live desktop failure.
- GREEN: the focused regression passes after the atomic source update.
- Live pre-fix: direct pointer interaction with the current desktop app scanned Zemeth and visibly reported 92 added while its database retained only the source.
- Live post-fix: after the non-destructive repair, the same desktop database retained one source, 92 tracks, a completed scan session, and scan errors; the running UI rendered the 92-track album library and reported a rescan of 0 added / 92 updated / 1 skipped.
- Portable final build launches against the persisted database and renders the library. Final Oracle review PASS: the compound SQLDelight operation is atomic, Android API-29-compatible, preserves cascade children, and overwrites all bound source fields.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.completedScanTerminalSourceUpdatePreservesPersistedChildren' --rerun-tasks --configuration-cache`: pass (`BUILD SUCCESSFUL in 9s`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: one broad attempt failed only at unrelated `PlaybackSessionCoordinatorTest.newerPlayingProgressSurvivesDelayedMutationCheckpoint`; its isolated normal and `--rerun-tasks` executions both passed. The changed SQLDelight persistence path has no dependency path to that in-memory playback checkpoint test.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 31s`).
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP and SQLDelight LSP are unavailable; forced Gradle generation/compilation/tests are the executable checks.
Changed files:
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibrarySource.sq`: non-destructive portable source upsert.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`: real scanner cascade regression plus legacy JVM database-opening coverage.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`: legacy unversioned JVM database version bootstrap from the earlier bisected startup failure.
Next owner: user for ordinary target-device/manual regression confirmation as desired.
Blockers: none for the desktop scan persistence defect. The broad JVM coordinator test is an unrelated intermittent timing failure that passed two isolated executions.
Commit: `8e975f6 fix: preserve scanned library persistence`.

## Fix - 2026-07-17 JVM legacy library database opening

Route: systematic-debugging + strict RED/GREEN TDD
Owner: implementation
Input: User bisected the desktop library scan regression to `f846b3f877e7c87a539377c896fbe7bbf199ceba` (`fix: open JVM library database through schema`).
Root cause:
- Pre-bisect JVM databases were initialized by direct `RhythHausDatabase.Schema.create(driver)` without setting SQLite `PRAGMA user_version`; populated historical databases therefore remain at version `0`.
- `f846b3f` changed the JVM factory to SQLDelight's schema-aware JDBC constructor. SQLDelight treats version `0` as an empty database and runs `Schema.create()`.
- Current-schema legacy databases already contain the unguarded `playlist` tables, so opening them threw `SQLITE_ERROR: table playlist already exists`; this prevented the desktop repository from opening and made scans appear not to persist/publish.
Fix:
- `LibraryDatabase.jvm.kt` now opens a short-lived JDBC driver with the existing foreign-key property, identifies only exact known unversioned RhythHaus table sets, stamps legacy v1 or current v2 `user_version`, closes that bootstrap driver, and then delegates all normal creation/migration to SQLDelight's schema-aware driver.
- Unknown, incomplete, empty, and already-versioned databases retain the prior schema-aware behavior; Android/iOS schema paths and persisted schema files are unchanged.
TDD and review:
- RED: added a reflection-free real SQLite fixture that creates the historic current schema, persists a source/track with `user_version = 0`, and verified the current factory failed with `SQLITE_ERROR: table playlist already exists`.
- GREEN: the same test verifies the real factory preserves both rows and permanently stamps `RhythHausDatabase.Schema.version`; forced rerun passed.
- Final Oracle review: PASS with no blocking correctness finding. Its requested persisted-version assertion was added and verified.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.legacyVersionZeroDatabaseCanBeReopenedWithoutLosingRows' --rerun-tasks --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s` after the reflection-free fixture cleanup).
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 9s`).
- `/usr/bin/xcrun xcodebuild -version`: pass (Xcode 26.6, build 17F113).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: iOS main compiled, then failed only at unchanged `AppScanCancellationTest.kt:64:28` and `:340:27` JVM-only `Thread` references; no iOS test pass is claimed.
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP is unavailable because `kotlin-ls` installation was previously declined; Gradle compilation/tests are the executable Kotlin checks.
Changed files:
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/LibraryDatabase.jvm.kt`: unversioned legacy schema bootstrap before the normal SQLDelight factory.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`: durable legacy-open regression, row preservation, and persisted-version coverage.
Next owner: user for optional live desktop rescan confirmation using the affected pre-fix database.
Blockers: no JVM/desktop/Android blocker. iOS simulator tests remain blocked by the unrelated common-test JVM-only `Thread` references.
Commit: `8e975f6 fix: preserve scanned library persistence`.

## Handoff - 2026-07-17 playlist screen Task 7 COMPLETE

Route: openspec+superpowers / Task 7 integration, verification, runtime QA, and Oracle adjudication
Owner: implementation and controller final whole-branch review complete through OpenSpec 7.4
Input: approved `playlist-screen` change, Tasks 1-6 through `1311151`, and Oracle adjudication that successful actual `LibraryDatabaseIosTest` execution is the sole explicit hard risk-acceptance gate.
Output:
- Real SQLDelight integration tests share one `LibraryDatabase` across library and playlist repositories and prove source removal/clear-library cascades are visible before playback reconciliation and playlist publication.
- Saved remove/reorder after queue resolution does not retroactively mutate the active queue; authoritative source removal preserves only surviving occurrences, while clear-library reconciliation empties the queue.
- Moved the JVM `Thread` dispatcher assertion and fixture from `commonTest` to `jvmTest` without production behavior changes, allowing the complete iOS simulator suite to compile and run.
- Actual `LibraryDatabaseIosTest.productionIosFactoryRejectsInvalidPlaylistEntryForeignKeys` executed 1/1 with zero skipped/failures/errors and observed the expected `SQLITE_CONSTRAINT` inside the passing assertion.
- Created byte-identical change-specific reports at `.superpowers/sdd/playlist-screen-task-1-report.md` through `playlist-screen-task-7-report.md`; generic scratch reports remain preserved and unstaged.
Verification:
- `openspec validate playlist-screen --strict`: pass (`Change 'playlist-screen' is valid`).
- `./gradlew :shared:verifyCommonMainRhythHausDatabaseMigration --configuration-cache`: pass.
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 116 actionable tasks, 11 executed, 105 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: pass (Xcode 26.6, build 17F113).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 3m 28s`; 35 actionable tasks, 7 executed, 28 up-to-date); native XML records iOS FK test 1/1.
- `GIT_MASTER=1 git diff --check`: pass.
Post-commit confirmation:
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 33s`; 35/35 tasks executed); fresh native XML timestamp `2026-07-17T10:19:56.156Z` records the iOS FK test 1/1 with zero skipped/failures/errors and expected `SQLITE_CONSTRAINT` output.
Runtime QA actually observed:
- Real 800x600 Chinese desktop Saved hub: localized Saved/Queue tabs, create action, empty state, create-dialog opening and dismissal.
- Real active Queue: current occurrence first and without mutation controls; localized track-named drag/move/remove controls on upcoming rows; first/last movement boundaries disabled; accessible move-down reordered one upcoming row; immediate remove deleted only its target; clear-upcoming opened confirmation stating current playback continues.
Unverified follow-up evidence, not passes:
- wide layout; separate light/dark states; English runtime localization; populated Saved create/rename/delete; duplicate saved rows; both add paths; saved and queue drag gestures; verified keyboard focus/submit; clear execution and stale-command rejection; pixel-level Now Playing overlap/spacing/contrast/CJK fidelity; target-device audible playback.
Review:
- Task-scoped read-only review PASS with no Critical or Important findings; independently reran lifecycle, dispatcher, and cancellation tests.
- Oracle adjudicated OpenSpec 7.1-7.3 complete with concerns. The controller's corrected final whole-branch review then passed all five lanes and closed OpenSpec 7.4.
Changed files:
- `shared/src/commonTest/.../AppScanCancellationTest.kt` and `shared/src/jvmTest/.../AppDispatcherJvmTest.kt`: portable/JVM test-source separation.
- `shared/src/jvmTest/.../PlaylistLifecycleIntegrationJvmTest.kt`: real lifecycle integration proof.
- `openspec/changes/playlist-screen/tasks.md`, `roadmap.md`, `progress.md`, and seven change-specific SDD reports: durable Task 7 evidence.
Next owner: OpenSpec archival only when explicitly requested. Do not archive or push in this session.
Blockers: none for OpenSpec completion or iOS FK proof. The listed manual/device states remain unverified follow-up evidence.
Commits: `a0219c2 test: isolate JVM dispatcher verification`; `0c525e9 test: verify playlist lifecycle integration`; evidence commits follow this handoff.
Final evidence/context correction:
- Restored generic `.superpowers/sdd/task-2-report.md` byte-for-byte from pre-playlist `fd95340`; durable playlist Task 2 evidence remains only under `.superpowers/sdd/playlist-screen-task-2-report.md`.
- Corrected OpenSpec Task 6.3, approved plan wording, and durable Task 6/7 reports to match reviewed role-free queue containers: accurate localized content/state descriptions, no false list-row role when Compose lacks one, and separate artwork image semantics.
- No production/tests changed. All previously listed wide/theme/English/pixel/device/audio limitations remain unverified follow-up evidence, not passes.

Final controller whole-branch review:

| Lane | Verdict | Confidence / maximum severity | Notes |
| --- | --- | --- | --- |
| Goal | PASS | High confidence | Approved playlist-screen scope and durable evidence align. |
| QA | PASS | High confidence | Listed runtime/manual subcases remain INCONCLUSIVE, not passes. |
| Code Quality | PASS | High confidence | No blocking quality finding. |
| Security | PASS | Maximum LOW | Non-blocking follow-ups are repository name/list/count limits aligned to the codec and a bounded/coalescing checkpoint queue; neither is implemented in this closure. |
| Context | PASS | High confidence | OpenSpec, plan, implementation, tests, and reports align after evidence corrections. |

- No Critical, Important, or Medium findings.
- INCONCLUSIVE and unverified: wide layout; separate light/dark states; English runtime localization; populated Saved create/rename/delete; duplicate saved rows; both add paths; saved and queue drag gestures; verified keyboard focus/submit; clear execution and stale-command rejection; pixel-level Now Playing overlap/spacing/contrast/CJK fidelity; target-device audible playback. None is claimed as a pass.
- Generic `.superpowers/sdd/task-2-report.md` remains restored to its unrelated pre-playlist content; the accessibility wording remains corrected to accurate role-free queue content/state semantics with separate artwork image semantics.
- OpenSpec `playlist-screen` is 29/29 complete. Final evidence is committed separately; no archive or push is performed.

## Follow-up - 2026-07-16 Android/iOS artwork slice crop

Route: systematic-debugging + strict RED/GREEN TDD
Owner: implementation; next owner is user/target-device QA
Input: User reported that the album artwork above the track list was cropped at the top and bottom on Android and iOS after the single-`LazyColumn` replacement, while macOS remained correct.
Root cause:
- `DrillDownArtworkPlane` requested `Modifier.size(expandedHeight)` inside upper and lower parents whose exact heights are only their clipped slice heights.
- Compose `size` honors incoming constraints, so the intended square was measured as a short rectangle in each slice. Coil then correctly applied `ContentScale.Crop` to that non-square destination, removing the source image's top and bottom.
- The lower `-collapseRange` offset therefore moved an already-cropped short image instead of revealing the bottom band of one shared square image.
Fix:
- Added a pure `ArtworkSlicePlaneGeometry` contract preserving the full square side, slice viewport height, and existing image offset.
- `DrillDownArtworkPlane` now uses outer `wrapContentSize(Alignment.TopStart, unbounded = true)` followed by inner square `size`, allowing the full square to measure independently of each short clipped viewport while anchoring its origin at top-start.
- Parent `clipToBounds`, the lower negative offset, `ContentScale.Crop`, scroll topology, no-artwork Miuix path, and macOS behavior remain unchanged.
TDD and verification:
- RED: focused `ArtworkCollapseTest` compilation failed only because `ArtworkSlicePlaneGeometry` and `artworkSlicePlaneGeometry` did not exist.
- GREEN: focused `ArtworkCollapseTest` passed (`BUILD SUCCESSFUL in 8s`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 11s`).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, build 17F113.
- `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`: pass (`BUILD SUCCESSFUL in 16s`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: iOS main compilation passed; test compilation remains blocked by unchanged JVM-only `Thread` references at `AppScanCancellationTest.kt:64:28` and `:340:27`. No iOS test pass is claimed.
- Kotlin LSP remains unavailable because installation was previously declined. Gradle compilation/tests are the executable language checks.
- `GIT_MASTER=1 git diff --check`: pass.
- Final Oracle source review: PASS with no Critical or Important findings.
Runtime acceptance:
- `adb devices -l`: no attached Android device/emulator.
- `xcrun simctl list devices booted`: no booted iOS simulator.
- Android/iOS visual acceptance for expanded, partial, and pinned artwork remains pending; no target-device visual pass is claimed.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`
- `openspec/changes/track-list-artwork-collapse/tasks.md`
- `progress.md`
Next safe action: build/run on Android and iOS, open an artwork-backed album or artist, and confirm the expanded square shows the complete image with a continuous upper/lower seam; also inspect partial and pinned states. Mark OpenSpec Task 5.5 complete only after both platforms pass.
Blockers: target-device visual acceptance only; unrelated iOS common-test `Thread` references. No focused JVM, JVM/desktop/Android matrix, iOS main compilation, diff-hygiene, or source-review blocker.

## Prototype gate - 2026-07-16 single-owner macOS artwork scrolling

Route: systematic-debugging / approved disposable architecture prototype / strict RED-GREEN
Owner: prototype gate complete; next owner is OpenSpec/Superpowers for the production architecture amendment and plan
Input: User approved the recommended recovery path after framework research and Oracle rejected further sibling-scroll/nested-scroll fixes.
Scope:
- Added a desktop-only diagnostic entry point with one `LazyColumn` and one `LazyListState` as the sole vertical input owner.
- The prototype uses a square placeholder artwork item, an in-list sticky toolbar with a clickable back button, 48 rows for deep scrolling, visible index/offset/collapse diagnostics, and a `Restore top` action that calls `scrollToItem(0, 0)`.
- Added a dedicated `runArtworkScrollPrototype` `JavaExec` task. Normal `MainKt`, Koin startup, packaging main class, shared production UI, OpenSpec artifacts, and the existing failed-attempt source files were not changed by the prototype.
- Added a desktop-local pure list-position-to-visual-state mapper and focused tests for expanded, partial, collapsed, restored, zero-range, and inverted-range states.
TDD evidence:
- RED: `./gradlew :desktopApp:test --tests 'com.eterocell.rhythhaus.ArtworkScrollPrototypeTest' --configuration-cache` failed at test compilation only because `ArtworkPrototypeVisualState` and `artworkPrototypeVisualState` did not exist.
- First GREEN attempt exposed missing direct desktop dependencies for the already-versioned Material 3 and material-icons artifacts; those catalog dependencies were added without changing versions or toolchains.
- GREEN: the focused prototype test passed (`BUILD SUCCESSFUL in 4s`).
Verification:
- `./gradlew :desktopApp:compileKotlin :desktopApp:test --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.
- `./gradlew :desktopApp:runArtworkScrollPrototype --configuration-cache` reached `:desktopApp:runArtworkScrollPrototype`; process PID `43045` was confirmed running with main class `com.eterocell.rhythhaus.ArtworkScrollPrototypeKt`.
- Launch log: `/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-artwork-prototype.log`.
- Orca runtime was unavailable (`runtime_unavailable`), so no synthetic gesture or screenshot result is claimed. Synthetic input would not substitute for the required physical trackpad evidence in any case.
Manual acceptance gate:
- PASS (user-confirmed): upward physical trackpad scrolling while the pointer is over non-button artwork pixels moves the sole lazy list.
- PASS (user-confirmed): after deep row scrolling, reverse physical trackpad scrolling reaches the top and fully restores the artwork without the prior dead boundary behavior.
- PASS (user-confirmed): `Restore top` returns the prototype to its top state (`index=0`, `offset=0`).
- PASS (user-confirmed): the back-arrow button closes the prototype.
Acceptance: disposable prototype build/run and all four requested physical macOS interaction checks PASS. This validates the single-`LazyColumn` input topology; no production fix is claimed yet.
Changed prototype files:
- `desktopApp/build.gradle.kts`: existing-version UI/test dependencies and isolated launch task.
- `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/ArtworkScrollPrototype.kt`: disposable one-owner experiment.
- `desktopApp/src/test/kotlin/com/eterocell/rhythhaus/ArtworkScrollPrototypeTest.kt`: pure mapper regressions.
Next safe action:
- Amend the implementation-specific OpenSpec/design wording, obtain plan approval, then replace the production artwork branch with the validated single-`LazyColumn` topology through TDD.
- Remove the disposable prototype after it has served as production-plan evidence; do not ship its entry point, task, or direct desktop-only UI dependencies as product functionality unless the approved plan explicitly retains them.
Blockers: production changes still require the OpenSpec/design amendment and an approved implementation plan. No prototype compile, focused test, launch, physical macOS interaction, or diff-hygiene blocker.
Commit: skipped; user did not request a commit, and this remains a disposable diagnostic experiment.

## Architecture stop - 2026-07-16 macOS artwork-collapse runtime failure

Route: systematic-debugging / architecture reassessment after three failed fixes
Owner: investigation complete; next owner is implementation only after a prototype and OpenSpec amendment
Input: User tested the clean, diagnostics-free desktop build after the third artwork-scroll attempt and confirmed both failures remained: trackpad scrolling over artwork was still dead, and reverse-scrolling the track list still could not restore the artwork.
Runtime acceptance:
- FAIL: artwork-zone trackpad scrolling did not move/collapse the artwork and list.
- FAIL: reverse trackpad scrolling after list movement did not restore the full-size artwork.
- The runtime reproduction todo is closed as failed/superseded, not passed. No fourth production fix was attempted.
Failed approaches:
1. Stabilized list geometry with fixed expanded padding, a visual collapse offset, and compensated viewport measurement; runtime restoration still failed.
2. Gated positive pre-scroll expansion on dynamic `LazyListState.canScrollBackward`; runtime restoration still failed.
3. Moved one nested-scroll connection to the common parent and made the z-indexed artwork chrome a sibling `Modifier.scrollable` sharing the list's `LazyListState`; both artwork-zone scrolling and restoration still failed in the clean runtime.
Confirmed evidence:
- Raw pointer tracing showed artwork-zone events reached the root and chrome hit-test branch but not the `LazyColumn` branch.
- List-boundary tracing captured a positive event where the child consumed part and post-scroll received the remainder (`+158 -> +53 child / +105 remainder`, with a second observed `+210 -> +55 / +155` sequence). This proved that a remainder can occur, but not that later boundary events reliably enter nested scroll.
- Compose Multiplatform 1.11.1 framework-source research found that Desktop `MouseWheelScrollingLogic` can reject a wheel delta from `LazyListState.canScrollForward/canScrollBackward` before opening the nested-scroll pipeline. Nested scroll propagates only from a dispatching descendant to ancestors; sibling scrollables sharing one state synchronize position but remain independent input owners.
- This framework behavior explains why parent `onPreScroll`/`onPostScroll` cannot guarantee macOS wheel restoration and why the z-indexed chrome remains an unreliable second input surface. JetBrains Compose Multiplatform issue #4975 reports the same Desktop/macOS nested-scroll wheel-boundary limitation.
Review verdict:
- The final source review of attempt three failed only because the `+210` trace test omitted its explicit matching pre-scroll assertion; it otherwise considered the source wiring coherent. The user's subsequent live failure supersedes that source-level assessment for acceptance.
- Oracle rejected another nested-scroll patch and selected one vertical input owner: make artwork the first header extent/item in the sole `LazyColumn`, derive collapse visuals from list position, keep collapsed chrome inside the same lazy topology, restore naturally at item `0` offset `0`, and leave the no-artwork Miuix branch unchanged.
- The observable product requirements remain valid, but implementation-specific OpenSpec language requiring an app-owned `NestedScrollConnection` and positive post-scroll restoration must be narrowly amended before production refactoring.
Next safe action:
- First build a disposable desktop-only single-`LazyColumn` prototype with placeholder square artwork, enough rows for deep scrolling, an in-list/sticky collapsed toolbar, the real back-button interaction shape, and scrollbar-to-`scrollToItem(0, 0)` behavior.
- Require live macOS trackpad/wheel proof that scrolling works over non-button artwork pixels, collapse is continuous, deep reverse scrolling restores the artwork without a dead boundary event, scrollbar-to-top restores item `0` offset `0`, and the back button remains clickable.
- If the prototype passes, amend the OpenSpec/design and create an approved implementation plan before replacing production architecture. If it fails, capture a fresh single-owner event trace instead of adding a platform input controller speculatively.
Verification and scope:
- Existing focused state-machine tests and JVM/desktop/Android compilation passed for attempt three, but they do not model or prove macOS wheel routing.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.
- No commit was created. Current failed-attempt source/test changes remain uncommitted and must not be presented as a completed fix.
Blockers: production implementation is blocked on the desktop prototype, live macOS input acceptance, and the required narrow OpenSpec/design update.

## Follow-up - 2026-07-16 track-list artwork immediate reverse restoration

Route: systematic-debugging + strict RED-GREEN TDD (second attempt after user-confirmed failure)
Owner: implementation
Input: User confirmed the first restoration fix was insufficient: after even a tiny artwork collapse, dragging downward could not restore the full image.
Corrected root cause:
- The prior handoff stabilized list layout geometry, but incorrectly assumed artwork expansion could wait for positive `onPostScroll` remainder.
- A tiny upward drag is consumed entirely by artwork collapse in `onPreScroll`, so the list remains at item zero. On the following downward drag, the `LazyColumn` can consume the positive delta before `onPostScroll`, leaving no remainder and no artwork expansion.
- The required ownership boundary is the current child scroll state: positive pre-scroll expands artwork only when `LazyListState.canScrollBackward` is false. When true, the list owns the downward delta until it returns to its start.
Fix:
- `ArtworkCollapseState` now receives a dynamic `canListScrollBackward` provider and reads it inside nested scroll.
- Negative pre-scroll still collapses artwork one-for-one.
- Positive pre-scroll expands artwork one-for-one only when the list cannot scroll backward; otherwise it consumes zero.
- Post-scroll no longer expands artwork, preventing ambiguous same-delta ownership after the child consumes movement.
- `DrillDownView` supplies `{ listState.canScrollBackward }`; no-artwork Miuix routing, stable fixed padding, compensated viewport layout, lazy artwork loading, and explicit scrollbar-to-top behavior remain unchanged.
Verification:
- RED: focused test compilation failed only because `ArtworkCollapseState` had no `canListScrollBackward` parameter.
- Connection-level regressions prove a `-1px` collapse followed by `+1px` at list top restores exactly to the expanded snapshot; a list that can scroll backward retains the positive delta; changing the provider to false on a later delta begins expansion; post-scroll consumes zero.
- First GREEN attempt encountered broad unresolved symbols across untouched common tests due stale Kotlin daemon/cache state; `:shared:compileKotlinJvm` still passed. After `./gradlew --stop`, an uncached `--rerun-tasks` focused run passed (`BUILD SUCCESSFUL in 29s`).
- Focused artwork/navigation group passed (`BUILD SUCCESSFUL in 1s`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s`; existing Android artwork metadata deprecation warning only).
- `GIT_MASTER=1 git diff --check`: pass.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined.
Runtime QA: User confirmation is still required on the real pointer/touch path; Orca runtime remained unavailable in the prior attempt, so no automated live gesture pass is claimed.
Next owner: user to retest tiny collapse followed immediately by downward restoration, then deeper list scrolling followed by return-to-top and a subsequent downward delta.
Blockers: manual target interaction confirmation only; no focused JVM, shared compile, JVM/desktop/Android matrix, or diff-hygiene blocker.
Commit: skipped; user did not request a commit.

## Handoff - 2026-07-16 Android Split APK releases final evidence

Route: openspec+superpowers / subagent-driven-development / final five-lane review
Owner: implementation complete; next owner is user for manual device install/upgrade validation
Input: Approved `android-split-apk-releases` change and roadmap item 22, including the user-approved AGP 9.3 AAB verifier revision that permits packaged `base/res/**` payloads required by `resources.pb`.
Output:
- Exact lowercase `rhythhaus.android.splitApk=true` produces `arm64-v8a`, `armeabi-v7a`, `x86_64`, and universal release APKs from one strict shared ABI contract; absent or every non-exact value preserves one ordinary APK.
- Cacheable APK verification checks AGP filters, exact TagLib slices, canonical `com.eterocell.rhythhaus / 0.1.0 / 100`, and existing signing policy. Actual release artifacts reported `signed: verified`.
- Independent `SingleArtifact.BUNDLE` verification creates a temporary proto archive with root manifest/table and 92 safe `base/res/**` payloads, then uses SDK `aapt2` and `apkanalyzer`; no `apksigner` runs on AABs.
- AGP 9.3 split-APK and AAB channels run separately. Full and abbreviated pure-channel tasks are classified consistently; mixed exact-split requests fail actionably.
Verification:
- Build-logic tests and plugin validation passed after focused test-only fix `1398191`; the explicit real-AAB probe also passed against the generated bundle.
- Reduced ABI and noninteger version-code commands failed with the expected actionable messages.
- Exact split `verifyReleaseApks`, run unchanged twice after a fresh cache: first stored, second literally reported `Reusing configuration cache.`; report contained four exact filters, exact native slices, canonical identity, and `signed: verified`.
- Independent `verifyReleaseAab`, run unchanged twice after a fresh cache: first stored, second literally reported `Reusing configuration cache.`; report contained one AAB, 92 resources, and canonical identity.
- Non-exact `True` produced one ordinary unfiltered signed APK; debug metadata contained one `SINGLE` unfiltered APK.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache --configuration-cache-problems=fail`: pass.
- `./init.sh`: JVM/desktop/Android passed and Xcode 26.6 was available; iOS main compiled, then common-test compilation failed only at unchanged `AppScanCancellationTest.kt:64:28` and `:340:27` unresolved `Thread` references. No iOS simulator test pass is claimed.
- `openspec validate android-split-apk-releases --strict`: pass (`Change 'android-split-apk-releases' is valid`). `GIT_MASTER=1 git diff --check`: pass.
Acceptance:
- Final five-lane review: goal/constraints PASS, hands-on artifact QA PASS, code quality PASS with no Critical/Important, security PASS with no Critical/High, and context mining found no missed technical requirement after this Task 6 lifecycle evidence.
- Non-blocking notes: sort duplicate-entry diagnostics; optionally add standalone `aR` and `vRelAab` TestKit cases; revalidate Gradle internal `NameMatcher` on a future Gradle upgrade.
- Scope controlled: no density/language splits, flavors, ABI version offsets, custom names, bundletool, publishing, dependencies, runtime behavior, TagLib JNI/source, signing-policy, or non-Android packaging changes.
Changed files:
- `gradle.properties`, `build-logic/convention/`, `taglib/build.gradle.kts`, `androidApp/build.gradle.kts`: ABI contract, opt-in splits, verifiers, and tests.
- `docs/superpowers/`, `openspec/changes/android-split-apk-releases/`, `.superpowers/sdd/android-split-apk-releases-task-1..5-report.md`: approved design/plan, durable requirements/tasks, and evidence.
- `roadmap.md`, `progress.md`: completed item 22 and this handoff; item 21 was preserved.
Commits: `7aea408`, `340c30d`, `713c60a`, `3a19a64`, `00ca186`, `2cc6140`, `8630c6a`, `c817cc5`, `3243fa7`, `8c257ee`, `ddd8213`, `8c655cc`, `270bcf8`, `1398191`, plus the final evidence commit.
Next owner: user for manual install/upgrade testing on representative ABIs, then OpenSpec archival only on explicit request.
Blockers: unchanged iOS common-test `Thread` references; no Android release, build-logic, JVM, desktop, OpenSpec, signing, cache, diff-hygiene, or final-review blocker.

## Handoff - 2026-07-16 track-list artwork reverse-scroll restoration

Route: systematic-debugging + strict RED-GREEN TDD
Owner: implementation
Input: User report that album artwork on the nested track-list screen could collapse after scrolling down but could not be restored by scrolling back.
Root cause:
- Artwork bytes and lazy load state remained available; the failure was not image cache eviction.
- Artwork expansion correctly consumed only positive `onPostScroll` remainder after the `LazyColumn` returned to its start, but the same collapse snapshot dynamically changed `contentPadding.top`. Increasing that padding during expansion moved the lazy list's start boundary and let the child consume the reverse delta required to continue expanding the artwork.
- The custom scrollbar uses `scrollToItem`, which bypasses nested-scroll dispatch, so returning it to index zero also required an explicit artwork-state reset.
Fix:
- Artwork-mode list padding now remains fixed at the expanded artwork height. A visual offset of `-collapseOffset` keeps the visible first-content edge equal to the current artwork chrome height without changing the lazy list's start boundary.
- The translated list child is measured taller by the same collapse offset while its wrapper retains the parent viewport, preserving the bottom render and hit-test extent.
- Scrollbar targets mapped to index zero call `ArtworkCollapseState.expandFully()` before `scrollToItem(0)`; other targets do not reset artwork.
- `rememberUpdatedState(onScrollToTop)` lets existing pointer-input coroutines observe the callback after lazy artwork changes from Loading to Available.
- Loading, unavailable, failed, and absent artwork continue using the existing Miuix scroll/chrome path; artwork loading and image cache code are unchanged.
Verification:
- Initial RED: focused `ArtworkCollapseTest` compilation failed only on missing fixed-padding/visual-offset and `expandFully` APIs.
- Review-fix RED: focused test compilation failed only on missing viewport-extension and scrollbar-target helpers.
- Focused GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- Focused artwork/navigation group: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 8s`).
- Shared compiler gate: `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- Fresh supported matrix: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- One earlier full-matrix attempt encountered the unrelated intermittent `PlaybackControllerTest.discreteCommandsEmitCompleteImmediateSnapshots` failure; its forced isolated rerun passed, CodeGraph found no call path from the changed UI files, and the fresh complete matrix passed.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, build `17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: iOS main compilation passed, then common-test compilation remained blocked by unchanged `AppScanCancellationTest.kt:64:28` and `:340:27` unresolved `Thread` references; no iOS simulator test pass is claimed.
- Kotlin LSP diagnostics were unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle compilation/tests are the executable Kotlin checks.
- `GIT_MASTER=1 git diff --check`: pass.
- Five-lane review initially found two blockers: moving the entire list could shorten its bottom hit region, and pointer input could capture the initial null Loading-state callback. Both were corrected as described above.
- Final Oracle re-review: PASS with no Critical or Important findings; viewport extent, callback freshness, top-only reset, and Miuix fallback were approved.
Runtime QA:
- `./gradlew :desktopApp:run --configuration-cache` reached `:desktopApp:run` without a reported crash.
- Live collapse/reverse-scroll/scrollbar screenshots and gestures were blocked because `orca status --json` reported the Orca runtime not running and `orca computer capabilities --json` returned `runtime_unavailable`. No live visual, gesture, pixel, or CJK pass is claimed.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapse.kt`: stable geometry helpers, explicit expansion, and top-target policy.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`: fixed artwork padding plus coordinated visual offset and compensated viewport measurement.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`: current callback capture and explicit scrollbar-to-top restoration.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/ArtworkCollapseTest.kt`: stable-boundary, viewport-extension, explicit expansion, and top-target regressions.
- `progress.md`: this handoff.
Scope: Unrelated `.superpowers/sdd/progress.md`, build-logic files/tests, and other concurrent workspace changes were preserved and not modified for this fix.
Next owner: user for manual pointer/trackpad validation on desktop or a target device, especially partial/full collapse, reverse expansion after the list reaches item zero, bottom-edge interaction, and scrollbar-to-top.
Blockers: manual runtime gesture/pixel validation unavailable; unrelated iOS common-test `Thread` references. No focused JVM, shared compile, JVM/desktop/Android matrix, diff-hygiene, or final-review blocker.
Commit: skipped; user did not request a commit.

## Handoff - 2026-07-15 track-list artwork collapse Task 3 verification evidence

Route: openspec+superpowers / SDD Task 3 verification / visual QA
Owner: implementation verification; final evidence commit is controller-owned
Input: Approved `track-list-artwork-collapse` change; implementation commits `01a1011`, `4ec83e9`, and Oracle-finding fix `eeae263`; clean task reviews and final Oracle gate.
Output:
- Verified the coordinated pinned-collapse implementation without modifying production or test code.
- Completed all OpenSpec tasks after available runtime/source visual QA, the resolved no-artwork classification finding, post-fix verification, and the final Oracle PASS. Unverified gesture/pixel/CJK behavior remains an explicit manual acceptance limitation rather than a claimed pass.
- Updated only roadmap item 21. Roadmap item 22 remains byte-for-byte `- [ ] build(android): 支持 SplitAPK`.
- Preserved `.superpowers/sdd/track-list-artwork-collapse-task-1-report.md` and `track-list-artwork-collapse-task-2-report.md`; added the change-specific Task 3 report rather than overwriting generic reports.
Verification:
- `openspec validate track-list-artwork-collapse --strict`: pass; exact output `Change 'track-list-artwork-collapse' is valid`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; `BUILD SUCCESSFUL in 37s`; `110 actionable tasks: 41 executed, 7 from cache, 62 up-to-date`; configuration cache stored. The only source warning was the existing Android `MediaMetadata.Builder.setArtworkData` deprecation at `PlaybackEngine.android.kt:474:17`.
- `/usr/bin/xcrun xcodebuild -version`: pass; `Xcode 26.6`, `Build version 17F113`.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: fail at `:shared:compileTestKotlinIosSimulatorArm64` after `:shared:compileKotlinIosSimulatorArm64` and `:shared:iosSimulatorArm64MainKlibrary` completed; exact errors are `AppScanCancellationTest.kt:64:28 Unresolved reference 'Thread'.` and `AppScanCancellationTest.kt:340:27 Unresolved reference 'Thread'.`; `BUILD FAILED in 11s`; `42 actionable tasks: 14 executed, 28 up-to-date`. This unrelated blocker was not fixed, and no iOS simulator test pass is claimed.
- `lsp_status`: Kotlin diagnostics unavailable; `kotlin-ls: missing`, with installation previously declined and zero active LSP clients. Gradle tests/compilation are the executable Kotlin checks.
- Initial `GIT_MASTER=1 git diff --check`: pass with no output. Initial `GIT_MASTER=1 git status --short` contained only the coordinator/user changes to the OpenSpec checklist and `roadmap.md`.
- Post-`eeae263` strict validation: `openspec validate track-list-artwork-collapse --strict` passed again with `Change 'track-list-artwork-collapse' is valid`.
- Post-`eeae263` focused verification: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.ArtworkCollapseTest' --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 471ms`; 26 actionable tasks: 4 executed, 22 up-to-date; configuration cache reused).
- Post-`eeae263` full matrix: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 9s`; 101 actionable tasks: 12 executed, 89 up-to-date; existing Android artwork metadata deprecation only).
- Post-`eeae263` Xcode check: `/usr/bin/xcrun xcodebuild -version` passed (`Xcode 26.6`; `Build version 17F113`).
- Post-`eeae263` iOS attempt: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` compiled iOS main, then failed at `:shared:compileTestKotlinIosSimulatorArm64` only at `AppScanCancellationTest.kt:64:28` and `:340:27` unresolved `Thread` (`BUILD FAILED in 5s`; 33 actionable tasks: 6 executed, 27 up-to-date). No iOS simulator test pass is claimed.
- Post-`eeae263` `GIT_MASTER=1 git diff --check`: pass with no output.
Visual QA:
- Launched `./gradlew :desktopApp:run --configuration-cache`; Orca runtime, Accessibility, and Screenshot permissions were available.
- Captured real desktop states at compact 800x600 and wide 1728x1084 using the existing local library: artwork album `A Thousand Suns`, no-artwork album `Apologize`, and artwork artist `7!!`. Accessibility trees confirmed real artwork nodes, title/subtitle, selectable tracks, back button, and scrollbar.
- Attempted forward scroll/collapse and reverse expansion. Orca reported synthetic input as unverified and the accessibility scroll value remained `0`, so these captures are not claimed as proof of partial/full collapse or reverse expansion.
- Initial visual pass A: PASS with medium overall confidence and high source confidence for shared geometry/chrome integrity; its no-artwork conclusion was later superseded by the Oracle Important finding and corrected by `eeae263`.
- Visual pass B: REVISE/RETEST with low confidence because the available reviewers could not decode screenshot pixels. OCR confirmed representative English/Chinese/Japanese content, but exact gaps, fades, CJK glyph precision, and gesture transitions were not visually approved. No reference baseline exists, so image-diff JSON is not applicable.
Acceptance:
- Requirement matched in source and automated geometry tests: yes. Runtime gesture feel and pixel-level visual fidelity remain unverified.
- Scope controlled: yes; no production/test/spec/plan changes in Task 3, and no attempt to fix the unrelated iOS blocker.
- Review history: the initial Oracle broad review found one Important no-artwork issue: representative track identity selected artwork-owned collapse before lazy loading proved that artwork bytes existed. Commit `eeae263` fixed it with explicit `Loading`, `Available`, and `Unavailable` states; only resolved `Available` bytes select coordinated collapse, while loading, missing, and failed artwork use Miuix.
- Focused post-fix re-review: specification `PASS`; code quality `APPROVED`. Two non-blocking Minors remain: no direct cancellation regression for `loadTrackArtworkState`, and the shared-classifier test name overstates route-level integration.
- Final post-fix Oracle gate: `PASS` with zero Critical and zero Important findings; safe to deliver with the documented manual visual-QA and unrelated iOS limitations.
Changed evidence files:
- `openspec/changes/track-list-artwork-collapse/tasks.md`: all implementation, verification, review, and evidence tasks complete.
- `roadmap.md`: completed only item 21 with verification and manual-QA limitations; item 22 preserved byte-for-byte.
- `progress.md`: this prepended handoff.
- `.superpowers/sdd/progress.md`: appended the Task 3 `DONE_WITH_CONCERNS` handoff without erasing history.
- `.superpowers/sdd/track-list-artwork-collapse-task-3-report.md`: exact command, runtime, review, and blocker evidence.
Git tracking note: `.gitignore:21` ignores `.superpowers/`. Task 2's report is already tracked, but `.superpowers/sdd/progress.md`, Task 1's report, and the new Task 3 report appear as `!!` under `GIT_MASTER=1 git status --short --ignored`; the controller must explicitly use `GIT_MASTER=1 git add -f` for intended ignored evidence rather than assuming the brief's plain `git add` will stage it.
Next owner: user for verified manual pointer/trackpad visual acceptance, then OpenSpec archival on explicit request; roadmap item 22 proceeds through its separate spec/plan workflow.
Blockers: existing iOS common-test JVM-only `Thread` references; unverified synthetic collapse/expand gestures; unavailable pixel/CJK screenshot inspection. No JVM, desktop, Android, OpenSpec, or diff-hygiene blocker.
Commits: implementation `01a1011`, `4ec83e9`, and Oracle-finding fix `eeae263`; final evidence commit follows this handoff.

## Handoff - 2026-07-14 hide Now Playing bar on settings information screens final evidence

Route: openspec+superpowers
Owner: implementation
Scope: Verification and durable evidence for approved `hide-now-playing-settings-about`; Task 2 covered verification and durable evidence only, while Task 1 source/test changes were reviewed but not modified in this session.
Verification:
- `openspec validate hide-now-playing-settings-about --strict`: pass (`Change 'hide-now-playing-settings-about' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 413ms`; 26 actionable tasks: 4 executed, 22 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s`; 110 actionable tasks: 12 executed, 98 up-to-date; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`; `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: fail at `:shared:compileTestKotlinIosSimulatorArm64` (`BUILD FAILED in 9s`; 42 actionable tasks: 6 executed, 36 up-to-date) because `AppScanCancellationTest.kt:64:28` and `AppScanCancellationTest.kt:340:27` each report `Unresolved reference 'Thread'`; iOS main compilation completed first and no iOS simulator test pass is claimed.
- `GIT_MASTER=1 git diff --check`: pass (no output).
- Scoped diff review: pass; suppression is exhaustive for `LibraryRoute.Settings`, `LibraryRoute.SettingsAbout`, and `LibraryRoute.OpenSourceLibraries`; every route outside that group retains existing behavior. The policy composes with existing visibility through logical conjunction and does not call playback APIs or remove the bar from composition.
- Final visual-QA fix: replaced the fixed 156 px translation estimate with the measured wrapper height and a pixel `IntOffset`, so the complete transparent hit region moves outside the window while the bar remains composed.
- Measured-offset RED: focused `LibraryNavigationTest` compilation failed only with unresolved `nowPlayingBarOffsetPx` references before production code was restored.
- Measured-offset GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`).
- Post-fix full matrix: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; existing Android artwork deprecation warning only).
- Post-fix iOS attempt: main compilation passed; common-test compilation remains blocked only at `AppScanCancellationTest.kt:64:28` and `:340:27` (`BUILD FAILED in 3s`), and no iOS simulator pass is claimed.
- Final source-level visual QA: PASS with high confidence after measured-height correction; no blocking findings, with runtime route-transition/accessibility validation retained as a manual gap.
- Final broad Oracle review: PASS with zero Critical or Important findings; approved as safe to deliver before the explicit commit request.
- Open Source Libraries follow-up RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest.settingsInformationRoutesSuppressNowPlayingBar' --configuration-cache` failed only at `LibraryNavigationTest.kt:506`, proving the route was still permitted before the policy change.
- Open Source Libraries follow-up GREEN: the focused regression, complete `LibraryNavigationTest`, and `:shared:compileKotlinJvm` passed (`BUILD SUCCESSFUL in 1s`).
- Follow-up full matrix: strict OpenSpec validation and `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 5s`; existing Android artwork deprecation warning only).
- Follow-up iOS attempt: iOS main compilation completed; common-test compilation remains blocked only by `AppScanCancellationTest.kt:64:28` and `:340:27` (`BUILD FAILED in 9s`), and no iOS simulator pass is claimed.
- `lsp_diagnostics`: not run because Kotlin LSP is unavailable by prior user decision; Gradle compilation/tests are the executable language checks.
Acceptance:
- Requirement matched: yes; Settings, About, and Open Source Libraries suppress the shell bar, while the scroll-derived visibility state and all non-settings route behavior remain unchanged.
- Scope controlled: yes; changes are limited to shared bottom-bar presentation/tests plus approved OpenSpec, plan/spec, roadmap/progress, and change-specific SDD evidence.
- Edge cases/risk reviewed: exhaustive route policy and true/false existing-visibility combinations are covered; playback/queue state is untouched. Route-transition animation remains a manual visual-QA limitation.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`: exhaustive route policy plus measured-offset helper.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`: route/scroll visibility composition and measured-height offset animation.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`: removed the obsolete fixed-height animation estimate after switching the shell to measured height.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`: route policy and measured-offset regression coverage.
- `openspec/changes/hide-now-playing-settings-about/tasks.md`: all seven implementation, verification, and follow-up tasks marked complete after evidence collection.
- `roadmap.md`: concise three-route completion, verification, iOS blocker, and manual-QA note.
- `progress.md`: this prepended completion handoff.
- `.superpowers/sdd/hide-now-playing-settings-about-task-1-report.md`: preserved change-specific Task 1 report.
- `.superpowers/sdd/hide-now-playing-settings-about-task-2-report.md`: preserved change-specific Task 2 command/status evidence and review-fix verification.
Next owner: user for manual route-transition visual QA, then OpenSpec archival on explicit request.
Blockers: iOS simulator tests remain blocked by the unrelated common-test JVM-only `Thread` references at `AppScanCancellationTest.kt:64:28` and `:340:27`; no OpenSpec, focused JVM, JVM/desktop/Android, or diff-hygiene blocker.
Commits:
- `4f8e793` (`docs: specify settings information bar visibility`)
- `7bab179` (`docs: propose settings information bar change`)
- `448b6da` (`docs: plan settings information bar visibility`)
- `e7eb6eb` (`feat: hide player bar on settings information screens`)
- Final roadmap/progress evidence: recorded by the commit containing this handoff.
Push: skipped; not requested.

Review fix:
- Preserved the current change evidence under the two change-specific report paths above before restoring the generic reports.
- Restored `.superpowers/sdd/task-1-report.md` and `.superpowers/sdd/task-2-report.md` exactly from `HEAD`; these generic reports are not part of this change's scope.
- Appended the same preservation/restoration verification to `.superpowers/sdd/hide-now-playing-settings-about-task-2-report.md`.

## Handoff - 2026-07-14 settings About + Open Source Libraries final evidence

Route: openspec+superpowers
Owner: implementation
Input: Approved `add-settings-about-page` change; Tasks 1-4 already implemented in the working tree (build metadata/catalog, route stack/Settings entry, shared About screen, Open Source Libraries screen with TagLib attribution override).
Output:
- Completed the build metadata/catalog, typed routes/Settings entry, shared About screen, and Open Source Libraries screen with deterministic TagLib attribution.
- Added recoverable attribution loading failure and retry behavior; malformed/empty catalogs no longer render as successful empty content.
- Added a centered 720 dp readable-width policy for the new About surfaces while preserving compact insets; enlarged the logo mark to 80 dp and centered multiline error copy.
- Preserved earlier generic SDD reports and stored this change's task evidence under `.superpowers/sdd/add-settings-about-page-task-1..4-report.md`.
Verification:
- `openspec validate add-settings-about-page --strict`: pass (`Change 'add-settings-about-page' is valid`).
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug :shared:compileKotlinIosSimulatorArm64 --configuration-cache`: pass after the responsive correction (`BUILD SUCCESSFUL in 16s`; existing Android MediaMetadata artwork deprecation warning only).
- `./gradlew :shared:verifyRhythHausVersionOverride -Prhythhaus.versionName=9.9.9 --configuration-cache`: pass (`BUILD SUCCESSFUL in 655ms`).
- Explicit `./gradlew :shared:exportLibraryDefinitions --rerun --configuration-cache`: pass and byte-stable; checked-in catalog MD5 remained `add5f05b1501c353a736f45aaa21eac4`.
- `.github/workflows/aboutlibraries-catalog.yml`: regenerates the catalog, runs its parser/TagLib tests, and rejects uncommitted catalog or override drift for relevant dependency/configuration changes.
- `GIT_MASTER=1 git diff --check`: pass.
- `./gradlew spotlessCheck --configuration-cache`: the changed `shared/build.gradle.kts` violation was corrected; the repository-wide gate remains blocked by pre-existing formatting violations in untouched TagLib and shared Android/playback files. No broad unrelated `spotlessApply` was run.
- Generic `.superpowers/sdd/task-1..4-report.md` files match `HEAD`; change-specific task reports are preserved separately.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, build `17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: blocked at the previously documented `AppScanCancellationTest.kt:64`/`:340` JVM-only `Thread` references; iOS main/resource compilation completed before common-test compilation failed, and no iOS simulator test pass is claimed.
- `lsp_diagnostics`: `kotlin-ls` not installed, installation previously declined (same blocker recorded in all four task reports).
- Final code-quality gate: PASS with no Critical, Major, or Minor findings after recoverable catalog failure handling, responsive modifier-order correction, OpenSpec reconciliation, and SDD evidence preservation.
- Focused security/supply-chain gate: PASS with no Critical or High findings after adding catalog regeneration/test/drift CI; upstream HTTP metadata links and repository-wide Gradle checksum policy remain non-blocking hardening suggestions.
- Source-level visual gate: PASS with high confidence for layout structure; screenshot/device confirmation remains manual because Orca computer-use was unavailable.
Acceptance:
- Requirement matched: yes, both `settings-about-page` and `open-source-attributions` OpenSpec spec deltas are satisfied by the implemented screens/routes/catalog per the task reports.
- Scope controlled: shared UI/resources, AboutLibraries build metadata/overrides, focused tests, and required workflow/evidence artifacts only.
- Manual QA limit: runtime/device visual QA (light/dark, logo rendering, license dialog, source-link open behavior) was not performed in any task; no simulator/device/desktop-run harness available in this environment, consistent with all four task reports.
Changed files:
- `shared/`, root Gradle catalog/configuration, and focused tests: shared About/attribution feature and build metadata.
- `docs/superpowers/` and `openspec/changes/add-settings-about-page/`: approved design, plan, requirements, and completed task state.
- `.superpowers/sdd/add-settings-about-page-task-1..4-report.md`: collision-free task evidence.
- `roadmap.md` and `progress.md`: completion and validation evidence.
Next owner: user for manual visual QA on target device/simulator/desktop window, then OpenSpec archival on explicit request.
Blockers: iOS simulator tests remain blocked by the pre-existing common-test `Thread` references above (unrelated to this change). No JVM/desktop/Android/OpenSpec blocker.
Commit: skipped; no explicit commit request was made.

## Handoff - 2026-07-14 playback session persistence final evidence

Route: openspec+superpowers / subagent-driven-development / final Oracle release gate
Owner: implementation
Input: Approved `persist-playback-session` change and roadmap item 18.
Output:
- Persist queue IDs, current track ID, playback position, repeat mode, and shuffle mode in the dedicated `playback_session.preferences_pb` DataStore; metadata, paths, artwork, URIs, engine objects, errors, and effective shuffled order are excluded.
- Process-owned restoration runs exactly once, reconciles against authoritative library state before publication, regenerates runtime shuffle order, and always completes paused without autoplay.
- One FIFO coordinator serializes restore, checkpoints, reconciliation, and flush barriers; only adjacent checkpoints coalesce, and persistence failures enter process-lifetime failed-safe behavior while callers and authoritative library publication still complete.
- Android MediaItem request tokens and iOS/JVM immutable generation/source provenance reject stale callbacks. macOS JNI operations, reset, and final release share one native-handle lifetime boundary; final release is permanent and reset cannot recreate native state or remote handlers.
Verification:
- `openspec validate persist-playback-session --strict`: pass (`Change 'persist-playback-session' is valid`).
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, build `17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: blocked by pre-existing JVM-only `Thread` references at `AppScanCancellationTest.kt:64` and `:340`; no iOS simulator test pass is claimed. iOS simulator main compilation passed separately.
- `GIT_MASTER=1 git diff --check`: pass.
- Complete review package `.superpowers/sdd/review-f98d5d0..616f7e5.diff`: 49 commits and 44 changed files through `616f7e5`.
- Final Oracle integration gate: PASS, zero Critical and zero Important findings; explicitly approved permanent macOS release, paused restore, FIFO failure safety, exactly-once process ownership, and platform provenance as safe to integrate.
- Post-integration verification initially exposed a test-only scheduling race in `staleGenerationCallbacksCannotMutateCurrentPlayback`: `awaitLoadCount(2)` observed the replacement load before it published `Paused`. The test now waits conditionally for paused readiness without weakening stale-callback assertions; it passed 20/20 stress iterations, the complete supported matrix, and a focused Oracle review with zero Critical/Important findings.
Acceptance:
- Requirement matched: yes for persisted session fields, paused/no-autoplay restore, reconciliation, checkpointing, failure safety, process ownership, and platform callback provenance.
- Scope controlled: no cloud sync, SQLDelight migration, UI redesign, Windows/Linux support, or abrupt-process-death durability guarantee was added.
- Manual QA limit: audible playback restoration and platform lock-screen/remote controls still require target-device validation.
Changed files:
- `openspec/changes/persist-playback-session/`: completed requirements, design, and task ledger.
- `shared/`, `androidApp/`, and `desktopApp/`: persistence, controller/coordinator, process integration, platform engines, native bridge, and regression coverage.
- `roadmap.md`: completed item 18 with verification and iOS blocker.
- `progress.md`: this final handoff.
Next owner: user for manual target-device playback/remote-control QA, then OpenSpec archival only on explicit request.
Blockers: iOS simulator tests remain blocked by the unchanged common-test `Thread` references above; no iOS simulator test pass is claimed. No JVM, Android host, desktop, Android, OpenSpec, diff-hygiene, or final-review blocker remains.
Commits: completion evidence `9811dc4` and `7cb751c`; post-integration test synchronization `50ade31`; this verification addendum is recorded immediately afterward.

## Handoff - 2026-07-14 restart current track selection final evidence

Route: openspec+superpowers
Owner: implementation
Input: Approved `restart-current-track-selection` change, Task 5 durable-evidence brief.
Output:
- Selecting the current track row restarts it at zero and plays without replacing the queue.
- Selecting a non-current row replaces playback with the exact visible queue from Library home, album, artist, or filtered Search results, then plays the selected track.
- Dedicated Now Playing play/pause controls still toggle transport. Repeat and shuffle values remain unchanged.
Verification:
- Focused integration: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --tests 'com.eterocell.rhythhaus.library.LibraryPlaybackSelectionTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 732ms`).
- Shared compiler gate: `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 331ms`).
- Full scoped command: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 529ms`, 99 tasks).
- `openspec validate restart-current-track-selection --strict`: pass.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, build `17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: blocked only by pre-existing `AppScanCancellationTest.kt:56:28` and `:99:27` unresolved `Thread` references. No iOS simulator pass is claimed.
- `GIT_MASTER=1 git diff --check`: pass.
- Task-level reviews and final Oracle release gate: PASS, with no Critical or Important findings. The sole follow-up is optional direct UI-level cross-surface queue-wiring coverage.
Acceptance:
- Requirement matched: yes for source behavior, focused integration, JVM, desktop, Android, and OpenSpec validation.
- Scope controlled: yes, production/test code, specs/design/plan, platform code, dependencies, and persistence remained unchanged during Task 5.
- Manual QA limit: audible playback and visible-queue behavior still require manual validation on target surfaces.
Changed files:
- `openspec/changes/restart-current-track-selection/tasks.md`: completed final review and durable-evidence tasks.
- `roadmap.md`: completed item 17 with behavior, validation, blocker, and manual-QA summary.
- `progress.md`: this final handoff.
Next owner: user for manual audible playback and queue QA, then OpenSpec archival only on explicit request.
Blockers: iOS simulator tests are blocked by the unchanged common-test `Thread` references above. Optional UI-level cross-surface queue-wiring tests remain a non-blocking follow-up.
Commit: recorded in the history immediately following this handoff.

## Handoff - 2026-07-13 Settings component padding alignment

Route: systematic-debugging + strict RED-GREEN TDD + source-level visual QA
Owner: implementation
Input: User report that Settings still had incorrect padding: the Appearance option and Settings back/title TopBar retained component-owned padding and were more inset than “管理音乐”.
Root cause:
- The Settings page already applies 16 dp horizontal content padding.
- Miuix 0.9.3 `SmallTopAppBar` additionally defaults to 26 dp `titlePadding` and 16 dp `navigationIconPadding`.
- Miuix `OverlayDropdownPreference` additionally defaults to `insideMargin = PaddingValues(16.dp)`.
- Those defaults stacked with the page inset, producing 42 dp title, 32 dp navigation, and 32 dp Appearance content insets versus the 16 dp “管理音乐” edge.
Fix:
- Extended `SettingsLayoutPolicy` with Settings-local component padding values.
- Settings TopBar now uses 0 dp title and navigation slot padding; global `RhythHausTopAppBar` defaults remain unchanged.
- Appearance preference now uses 0 dp horizontal and 16 dp vertical `insideMargin`.
- Preserved `safeContentPadding()`, page spacing 16/8/12/8, 44 dp back target, callbacks, semantics, source management, and dropdown behavior.
Verification:
- RED: `SettingsScreenTest` failed with unresolved `topBarTitlePadding`, `topBarNavigationIconPadding`, `appearanceHorizontalInsidePadding`, and `appearanceVerticalInsidePadding` before production changes.
- Focused GREEN: `SettingsScreenTest` plus `LibrarySourceManagementTest`: pass.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s`; existing Android artwork deprecation warning only).
- `git diff --check`: pass.
- Independent code review: PASS with no Critical, Important, or Minor findings.
- Two source-level visual QA Oracle passes: PASS with MEDIUM confidence; no runtime screenshots were available.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt`
- `roadmap.md`
- `progress.md`
Next owner: user for live visual confirmation on a representative target device/window.
Blockers: no automated JVM/desktop/Android blocker. Kotlin LSP unavailable by prior user choice; runtime screenshots unavailable.
Commit: code/test and documentation evidence committed separately after explicit user integration request.

## Handoff - 2026-07-13 Library home chrome and Settings spacing

Route: openspec+superpowers / subagent-driven-development / TDD / visual QA
Owner: implementation
Input: Roadmap item 16: remove the Library home Nested Top Bar and reduce oversized Settings insets/padding.
Output:
- Removed the Library home's scroll-triggered `NestedScrollBlurChrome`, its home-only state model, and obsolete progression tests without adding a replacement bar.
- Preserved the platform-derived top system inset, in-content Library header, browse content, Now Playing spacing, and scroll-driven Now Playing bar visibility.
- Preserved album/artist `DrillDownMiuixScrollChrome`, artwork transitions, safe-start back inset, title treatment, and Miuix scroll behavior.
- Added `SettingsLayoutPolicy` and applied the approved compact values: 16 dp horizontal, 8 dp vertical, 12 dp inter-item, and 8 dp final bottom padding while retaining `safeContentPadding()`.
Verification:
- Settings RED: focused test failed with unresolved `CompactSettingsLayoutPolicy`; GREEN plus `LibrarySourceManagementTest`: pass (`BUILD SUCCESSFUL in 9s`).
- Library RED: focused test failed with unresolved `libraryHomeTopContentPadding`; GREEN: pass (`BUILD SUCCESSFUL in 4s`).
- `LibraryNavigationTest`: pass (`BUILD SUCCESSFUL in 1s`).
- `:shared:compileKotlinJvm`: pass (`BUILD SUCCESSFUL in 444ms`).
- `openspec validate library-home-settings-spacing --strict`: pass.
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL in 21s`).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, build `17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: blocked by unchanged common-test errors at `AppScanCancellationTest.kt:56:28` and `:99:27`, both `Unresolved reference 'Thread'` (`BUILD FAILED in 1s`).
- `git diff --check`: pass.
- Task reviews: both spec compliant and quality approved; final whole-branch Oracle review PASS with no findings.
- Visual QA: two source-level Oracle passes PASS with MEDIUM confidence. Live screenshots were not captured because the desktop run exited before capture and the Orca runtime was not running.
Acceptance:
- Requirement matched: yes for source, tests, JVM, desktop, Android, and OpenSpec validation.
- Scope controlled: shared Library/Settings UI and tests plus required workflow/evidence artifacts only.
- Accessibility: safe areas, existing semantics, 44 dp icon targets, and 48 dp primary buttons preserved.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`: compact immutable spacing policy and wiring.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenTest.kt`: exact-value layout policy coverage.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`: retained top inset and removed home chrome state/overlay.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`: removed dead home-only chrome composable.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`: removed dead home-only chrome state policy.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`: safe-top coverage and obsolete test removal.
- `docs/superpowers/specs/2026-07-13-library-home-settings-spacing-design.md`: approved design.
- `docs/superpowers/plans/2026-07-13-library-home-settings-spacing.md`: executable TDD plan.
- `openspec/changes/library-home-settings-spacing/`: proposal, design, requirements, tasks, and evidence.
- `roadmap.md`: item 16 completion and manual-QA limitation.
- `progress.md`: this handoff.
Next owner: user for manual visual QA on Android/iOS and compact/wide desktop windows, then OpenSpec archival on explicit request.
Blockers: automated iOS tests remain blocked by the pre-existing common-test `Thread` references above; live visual capture was unavailable. No JVM/desktop/Android/OpenSpec blocker.
Commits: `8804a7b` (`feat: compact settings layout spacing`), `cd72078` (`feat: remove library home nested chrome`), plus documentation/evidence commits created at workflow completion.

## Handoff - 2026-07-13 Android oversized lazy-artwork CursorWindow crash

Route: systematic-debugging + strict RED-GREEN TDD
Owner: implementation
Input: Android crash after adding a second music folder: `SQLiteBlobTooBigException` in generated `SelectArtworkForTrackQuery.execute`, called by `SqlDelightLibraryRepository.artworkForTrack()` and lazy Compose artwork loading.
Root cause:
- Routine library queries already excluded artwork BLOBs, but the lazy `selectArtworkForTrack` query still selected one complete embedded-art BLOB into a single Android `CursorWindow` row.
- Android failed before Kotlin received the result when one track's artwork exceeded the cursor-window capacity. The second folder was incidental; it introduced a track with sufficiently large embedded artwork.
Fix:
- Replaced the full-BLOB query with `selectArtworkMetadataForTrack`, which reads only `length(artworkBytes)` and MIME, plus `selectArtworkChunkForTrack`, which reads bounded SQLite `substr(BLOB, offset, length)` chunks.
- `SqlDelightLibraryRepository.artworkForTrack()` now requests at most 256 KiB per query, preallocates the exact `ByteArray`, and reassembles chunks with `copyInto` off the UI thread through the existing lazy artwork loader.
- Missing, short, invalid-length, or over-`Int.MAX_VALUE` artwork returns `null` rather than partial/corrupt data. No table shape or SQLDelight migration changed.
Verification:
- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.largeArtworkIsLoadedLazilyInMultipleBoundedChunks' --configuration-cache` failed with unresolved bounded-query/helper APIs (`BUILD FAILED in 2s`).
- Focused 3 MiB + 137 byte regression: pass; 13 bounded reads, exact byte reassembly, and MIME preservation (`BUILD SUCCESSFUL in 5s`; fresh QA rerun also passed).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 10s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' --configuration-cache --rerun-tasks`: pass (`BUILD SUCCESSFUL in 21s`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `git diff --check`: pass for the implementation; independent code review and QA: PASS.
Changed files:
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`: metadata and bounded artwork-chunk queries; removed full-BLOB lazy query.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`: bounded chunk count, validation, allocation, and exact reassembly.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`: 3 MiB+ chunked artwork regression.
- `progress.md`: this handoff.
Next owner: user for launch validation on the Android device/database that previously crashed, including artwork from both configured folders.
Blockers: no automated JVM/desktop/Android blocker. Live Android CursorWindow validation was not available; Kotlin LSP remains unavailable because installation was previously declined.
Commit: `11778a3` (`fix: load large artwork in bounded chunks`) and `ffb14f3` (`docs: record artwork cursor window fix evidence`). This `progress.md` handoff is left uncommitted because the user did not request an additional commit; unrelated user edits in `roadmap.md` remain untouched.

## Handoff - 2026-07-13 multi-library folders final evidence

Route: openspec+superpowers / final post-review evidence
Owner: implementation
Input: Finalize durable evidence for all post-review fixes to `multi-library-folders` without modifying source, tests, specs, design, plans, or SDD reports.
Output:
- Final review fixes are recorded: the empty-library Add Folder card uses the progress-plus-job mutation gate; Android SAF persists read-only access; Android SAF URI and JVM canonical-path identities encode their full values while exact-handle normalization preserves legacy IDs and creation times; picker failures and unnamed labels are generic and neutral.
- Lifecycle fixes are recorded: remove and clear release Android persisted SAF access only after successful repository mutation; `clearAll()` is a single child-first transaction; cancellation before candidate import releases metadata resources.
- Roadmap item 15 remains complete for Android and desktop JVM/macOS additive sources, with iOS intentionally limited to one app-local source.
Verification:
- `openspec validate multi-library-folders --strict`: pass (`Change 'multi-library-folders' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`; 34 actionable tasks: 5 executed, 29 up-to-date).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`; 108 actionable tasks: 5 executed, 103 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`; `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: blocked by pre-existing common-test compilation errors: `AppScanCancellationTest.kt:56:28 Unresolved reference 'Thread'` and `AppScanCancellationTest.kt:99:27 Unresolved reference 'Thread'`; both calls exist unchanged at the supplied Task 4 base. Result: `BUILD FAILED in 9s`, 41 actionable tasks: 12 executed, 29 up-to-date.
- `git diff --check`: pass (no output).
- Race-fix RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --configuration-cache` failed before implementation with unresolved `sourceMutationsAllowed` (`BUILD FAILED`).
- Race-fix GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 831ms`).
- Race-fix JVM/desktop/Android: `./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- Race-fix `git diff --check`: pass (no output); final re-review: PASS.
- Latest focused suites: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibrarySourceManagementTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`; 34 actionable tasks: 7 executed, 27 up-to-date).
- Latest scoped full verification: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`; 99 actionable tasks: 12 executed, 87 up-to-date; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
Acceptance:
- Requirement matched: yes for automated repository, source-management, JVM, desktop, Android, and OpenSpec checks.
- Scope controlled: only `openspec/changes/multi-library-folders/tasks.md`, `progress.md`, and `roadmap.md` change in this evidence handoff.
- Deferred non-blocking legacy scanner hardening: JVM symlink containment and cycle detection, lossless `sourceLocalKey` semantics, and persisted terminal-message sanitization.
Changed files:
- `docs/superpowers/specs/2026-07-10-multi-library-folders-design.md`: approved design artifact.
- `docs/superpowers/plans/2026-07-10-multi-library-folders.md`: approved implementation plan.
- `openspec/changes/multi-library-folders/`: durable proposal, design, requirements, and task evidence.
- `progress.md`: this handoff.
- `roadmap.md`: item 15 completion and concise platform/manual-QA limitation.
- `.superpowers/sdd/progress.md`: Task 4 review ledger.
Next owner: user for manual Android SAF picker/access-release and Android/desktop add/rescan/remove validation, plus iOS app-local rescan validation. OpenSpec archival only on explicit request.
Blockers: iOS simulator tests remain blocked by the pre-existing common-test `Thread` references above. No iOS automated test, live device, or manual visual QA pass is claimed.
Commit: initial workflow `f8621b9`, `d1d33cc`, `2dcf856`, `e507b30`; late review/evidence `92a20fc..52f53d9` (`92a20fc`, `79d16b5`, `539aeff`, `3111e5f`, `4812308`, `c157233`, `1d9759d`, `fc2fb25`, `9187499`, `2e84847`, `564822b`, `104e087`, `f08e810`, `9de167c`, `733e560`, `c54e232`, `52f53d9`); final durable evidence `389aeb7` (`docs: finalize multi-library folders evidence`).

## Handoff - 2026-07-09 drill-down track-list safe inset and scroll background

Route: systematic-debugging + visual QA
Owner: implementation
Input: User report: track-list back button is too close to the start edge; apply safe insets, and make nested-scroll chrome avoid a solid background when unscrolled while fading to a solid background after scroll.
Root cause:
- `DrillDownMiuixScrollChrome` opted out of Miuix default window insets and used `navigationIconPadding = 0.dp`, so the back button could hug the start edge on safe-area devices.
- The artwork drill-down chrome rendered artwork/scrim/title chips but did not have a scroll-driven solid background layer that is transparent at rest and fades in with nested-scroll collapse progress.
Fix:
- Added a safe-drawing start inset helper using `WindowInsets.safeDrawing` plus layout-direction-aware `calculateStartPadding`, then applied that value plus 12.dp to the Miuix `TopAppBar` navigation icon padding.
- Added an `animateFloatAsState`-driven paper overlay in the artwork chrome path, with alpha `0f` when unscrolled and fading toward near-solid as `collapsedFraction` increases.
Verification:
- `lsp_diagnostics` for `LibraryChrome.kt`: blocked because `kotlin-ls` is not installed and the user previously declined installation.
- Initial `./gradlew :shared:compileKotlinJvm --configuration-cache`: failed with unresolved `calculateStartPadding`; fixed by importing `androidx.compose.foundation.layout.calculateStartPadding`.
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`).
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `git diff --check`: pass (no output).
- Visual QA source-level Oracle passes: both PASS with MEDIUM confidence; no rendered screenshot/device capture was available, so live visual validation remains manual.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`: safe start inset for drill-down back button and scroll-fade solid artwork chrome layer.
- `progress.md`: this evidence record.
Next owner: user for live visual QA on target device/simulator, especially perceived collapsed opacity and long/CJK title chip wrapping.
Blockers: no automated compile blockers; runtime screenshot/device validation was not available in this session.
Commit: skipped; user did not ask to commit.

Follow-up adjustment:
- User reported the back button's own solid circular background was still visible when not scrolled.
- Root cause: the artwork-mode `IconButton` still hardcoded `HausColors.current.paper.copy(alpha = 0.78f)`, independent of scroll progress.
- Fix: tied the artwork-mode back-button fill to the same animated scroll alpha, so it is transparent at rest and fades in with the solid chrome background while preserving the 44.dp target and safe-start padding.
- Additional verification: `lsp_diagnostics` remained blocked because `kotlin-ls` is not installed and the user previously declined installation; `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL in 4s`); `git diff --check` passed.

## Handoff - 2026-07-09 iOS system media artwork after lazy loading

Route: systematic-debugging + TDD
Owner: implementation
Input: User report: after the lazy-load library artwork commit, iOS artwork no longer shows in system media controls.
Root cause:
- The lazy-loading artwork change intentionally made routine library/queue track rows metadata-only (`artworkBytes = null`) to avoid eager BLOB materialization.
- `PlaybackController` still handed those metadata-only `PlayableTrack`s directly to the platform engine.
- On iOS, `IOSPlaybackEngine.updateNowPlayingInfo()` only calls `NowPlayingArtworkBridge.provider?.setArtwork(..., artworkBytes = track.artworkBytes)` from the loaded `PlayableTrack`, so the Swift `RhythHausArtworkProvider` removed artwork from `MPNowPlayingInfoCenter` when playback loaded a metadata-only track.
Fix:
- Added an optional `artworkLoader` seam to `PlaybackController` and wired Koin production construction to `LibraryRepository.artworkForTrack(trackId)?.bytes`.
- `PlaybackController` now resolves lazy artwork inside the existing playback load coroutine before calling `engine.load(trackWithArtwork)`, leaving routine library rows metadata-only while giving platform engines artwork for system media metadata.
- Added `PlaybackControllerTest.playbackLoadsLazyArtworkBeforeHandingTrackToEngine` regression coverage proving a metadata-only queue track is handed to the engine with lazily loaded artwork bytes.
Verification:
- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest.playbackLoadsLazyArtworkBeforeHandingTrackToEngine' --configuration-cache` first failed with `No parameter with name 'artworkLoader' found`, proving the missing lazy-artwork playback seam.
- Targeted: same command passed after implementation (`BUILD SUCCESSFUL in 3s`).
- Controller suite: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- iOS compile: `./gradlew :shared:compileKotlinIosSimulatorArm64 --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- Desktop/Android focused build: `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- `git diff --check`: pass (no output).
- Wider `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` remains blocked only by existing macOS-native `JvmPlaybackEngineTest.nativeMacPlaybackEngineLoadsGeneratedWavFile` and `nativeMacPlaybackEnginePublishesProgressWhilePlaying` failures; desktop and Android tasks completed in that run.
- `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSNowPlayingInfoTest' --configuration-cache` is blocked by unrelated common-test iOS compilation errors in `AppScanCancellationTest.kt` (`Thread` unresolved on iOS), so iOS validation used the focused iOS main compilation above.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`: loads lazy artwork before platform engine load.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`: wires playback artwork loader to repository lazy artwork lookup.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt`: regression test for lazy artwork handoff to playback engine.
- `progress.md`: this evidence record.
Next owner: user for manual iOS Control Center / lock-screen artwork validation on a simulator or device with scanned tracks containing embedded artwork.
Blockers: automated iOS test task blocked by existing common-test `Thread` usage; full JVM suite blocked by existing macOS-native playback test failures.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-09 clear-library dialog route and glass background

Follow-up adjustment:
- User reported the route-level dialog still showed a screen-to-screen transition animation and asked for the dialog to behave like a real alert dialog on Settings.
- Changed Settings to own local `showClearLibraryDialog` state; the clear button now opens `AnimatedClearLibraryDialogRoute` inside `SettingsScreen` instead of pushing `LibraryRoute.ClearLibraryDialog`.
- `ClearLibraryDialog` is no longer used for normal Settings clear-library flow; if encountered via old/stale navigation state, it immediately pops back.
- `routeRequiresInWindowContentAnimation(ClearLibraryDialog)` now returns false, and the regression test asserts the dialog route does not require route content animation.
- Additional verification: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest.clearDialogDoesNotUseRouteContentAnimation' --configuration-cache`: RED before implementation, then pass (`BUILD SUCCESSFUL in 3s`); `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 670ms`); `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`); `git diff --check`: pass.

Follow-up adjustment 2:
- User reported clearing the library blocks the UI thread.
- Added `clearLibraryInBackground(...)`, which runs `repository.clearAll()` plus the post-clear repository read on a supplied background dispatcher, then applies the Compose state update after the background work returns.
- `App()` now launches the clear operation from the remembered coroutine scope and passes `Dispatchers.Default`, instead of calling `repository.clearAll()` synchronously from the UI callback.
- Added `AppScanCancellationTest.clearLibraryRunsRepositoryWorkOnProvidedDispatcher`, which captures the thread used by `clearAll()` and proves it differs from the caller thread while the UI update receives an empty track list.
- Additional verification: RED `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest.clearLibraryRunsRepositoryWorkOnProvidedDispatcher' --configuration-cache` failed with unresolved `clearLibraryInBackground`; after implementation the same test passed (`BUILD SUCCESSFUL in 3s`); `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 607ms`); `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL in 338ms`); `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 3s`); `git diff --check` passed.

Follow-up adjustment 3:
- User reported repeated Android `CursorWindow Failed alloc ... NO_MEMORY` logs when opening the app, likely while loading the library.
- Root cause refinement: previous mitigation still selected artwork BLOBs up to 512 KiB for every routine track row. Many rows with individually bounded artwork can still fill Android CursorWindow during app startup/library refresh.
- Changed routine SQLDelight track-list/update-lookup queries (`selectAllTracks`, `selectTracksForSource`, `selectTrackBySourceKey`) to project metadata only and always return `NULL` for `artworkBytes`/`artworkMimeType`, avoiding BLOB materialization in library startup paths.
- Added `SqlDelightLibraryRepositoryJvmTest.boundedArtworkIsNotLoadedWithRoutineTrackRows` proving even 128 KiB artwork is not loaded by `tracks()`.
- Verification: RED `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.boundedArtworkIsNotLoadedWithRoutineTrackRows' --configuration-cache` failed at `SqlDelightLibraryRepositoryJvmTest.kt:76` before SQL change; after implementation the same test passed (`BUILD SUCCESSFUL in 4s`); `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 1s`); `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL in 353ms`); `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 3s`); `git diff --check` passed.

Follow-up adjustment 4:
- User corrected the previous artwork mitigation: artwork must not disappear from the library; it should be lazy-loaded instead of eagerly loaded with every library row.
- Added a `TrackArtwork` model and `LibraryRepository.artworkForTrack(trackId)` so routine track-list queries can remain metadata-only while visible artwork surfaces can fetch one track's artwork on demand.
- Added SQLDelight `selectArtworkForTrack` and repository implementation for lazy BLOB lookup by track id.
- Kept `tracks()` / `tracksForSource()` metadata-only to avoid Android CursorWindow pressure, but wired UI artwork rendering through `LazyTrackArtworkImage` and `LocalTrackArtworkLoader` so thumbnails/cards/Now Playing/artwork chrome load artwork only when those composables enter composition.
- Updated in-memory repository to mirror production behavior: routine `tracks()` strips artwork, `artworkForTrack()` returns stored artwork.
- Added `ArtworkLazyLoadingTest.routineLibrarySnapshotDoesNotCarryArtworkButRepositoryCanLoadItByTrackId` and `SqlDelightLibraryRepositoryJvmTest.artworkCanBeLoadedLazilyByTrackId` to prove metadata-only list loading plus lazy artwork retrieval.
- Verification: RED `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest.routineLibrarySnapshotDoesNotCarryArtworkButRepositoryCanLoadItByTrackId' --configuration-cache` failed with unresolved `artworkForTrack`; after implementation passed (`BUILD SUCCESSFUL in 4s`); `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.artworkCanBeLoadedLazilyByTrackId' --configuration-cache` passed (`BUILD SUCCESSFUL in 1s`); combined `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --tests 'com.eterocell.rhythhaus.library.ArtworkLazyLoadingTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 1s`); `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 7s`); `git diff --check` passed.

Route: systematic-debugging + TDD
Owner: implementation
Input: User report: Settings page click "清空资料库" shows the confirmation dialog back on the Library page; also requested the dialog background use miuix-blur and remove the white background layer.
Root cause:
- `LibraryRoute.ClearLibraryDialog` was pushed as a normal route, but `LibraryRouteContent` treated that route like home content. In compact layout the dialog route therefore rendered `homeContent(...)` under the route overlay instead of the previous Settings route.
- The clear-library dialog card used an opaque `HausColors.current.panel` Miuix `Card` fill, so it kept a solid/white panel layer instead of sampling the recorded Miuix blur backdrop.
Fix:
- Added `visibleContentRouteForOverlay(...)` so the clear-library overlay keeps the previous route, e.g. Settings, as the visible in-window content while the dialog is current.
- Moved the compact clear-library overlay outside `AnimatedContent` and pass the root recorded backdrop into the dialog.
- Applied `rhythHausLiquidGlass(...)` to the dialog card with a transparent Miuix `Card` fill and a tinted fallback color for unsupported blur paths.
- Adjusted list/detail recording so overlays remain siblings of the backdrop-recorded content instead of being recorded into their own sampled backdrop.
Verification:
- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest.clearDialogUsesPreviousRouteAsVisibleContent' --configuration-cache`: failed with unresolved `visibleContentRouteForOverlay`, proving the missing routing behavior.
- Targeted: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest.clearDialogUsesPreviousRouteAsVisibleContent' --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- Navigation suite: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- Focused compile: `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 719ms`).
- `git diff --check`: pass (no output).
- Wider platform command: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: failed only in unrelated existing `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` at `JvmPlaybackEngineTest.kt:190`; `:desktopApp:compileKotlin` and `:androidApp:assembleDebug` completed. Re-running just that playback test reproduced the same failure.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDialogs.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/ui/LibraryNavigationTest.kt`
- `progress.md`
Next owner: user for manual visual QA of Settings -> 清空资料库 on target device/simulator, especially blur appearance on Android API levels that support RuntimeShader.
Blockers: full `:shared:jvmTest` remains blocked by unrelated `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` failure.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-09 Android SQLite CursorWindow large artwork crash

Route: systematic-debugging + TDD
Owner: implementation
Input: User crash report: Android `SQLiteBlobTooBigException: Row too big to fit into CursorWindow` in `SqlDelightLibraryRepository.tracks()` while Compose initializes `App()`.
Root cause:
- `LibraryTrack.sq` used `SELECT *` for `selectAllTracks`, `selectTracksForSource`, and `selectTrackBySourceKey`, so every library refresh loaded embedded album-art BLOBs into Android's CursorWindow.
- A library containing enough rows and/or large embedded artwork can exceed the Android CursorWindow row/window limit before UI code can render or Coil can cache thumbnails.
Fix:
- Changed the track-list SQLDelight queries to project scalar track metadata plus only bounded embedded artwork (`<= 524288` bytes); larger artwork returns `NULL` for `artworkBytes`/`artworkMimeType`, keeping oversized BLOBs out of Android CursorWindow during routine list reads and source-key update lookups while preserving small artwork thumbnails.
- Added a JVM SQLDelight regression test proving a row with 600 KB artwork can be inserted but `tracks()` does not materialize artwork bytes or MIME type.
Verification:
- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.oversizedArtworkIsNotLoadedWithTrackRows' --configuration-cache`: failed at `SqlDelightLibraryRepositoryJvmTest.kt:50` because `tracks()` still loaded artwork bytes.
- Targeted: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest.oversizedArtworkIsNotLoadedWithTrackRows' --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- Repository suite: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.SqlDelightLibraryRepositoryJvmTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- Focused platform build: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `git diff --check`: pass (no output).
Changed files:
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/LibraryTrack.sq`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepositoryJvmTest.kt`
- `progress.md`
Next owner: user for manual Android launch validation against the library that previously crashed.
Blockers: none for automated JVM/desktop/Android verification. Live Android validation against the crashing device library was not run in this session.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-09 scan progress live updates

Route: systematic-debugging + TDD
Owner: implementation
Input: User report: "Fix: Scan process is not updated in the scanning process, at least on Android"
Root cause:
- `LibraryScanner` updated the repository after each `PlatformScanEvent`, but `App()` only assigned Compose `scanProgress` before starting the scan and after `scanner.scan(...)` returned.
- The Android SAF scanner emits folder/file events during traversal, but those intermediate sessions never reached Compose state, so the scanning card counters appeared frozen until completion.
Fix:
- Added an `onProgress: (ScanProgress) -> Unit` callback to `LibraryScanner.scan(...)` with a no-op default to preserve existing callers.
- `LibraryScanner` now emits progress at session start, after each scan event, on cooperative cancellation, on completion, and on failure.
- `App()` passes the progress callback and updates `scanProgress` on `Dispatchers.Main` while scanning continues on `Dispatchers.Default`.
- `ScanningCard` now receives and displays `latestItem` so users can see the currently traversed folder/file path in addition to counters.
Verification:
- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerReportsProgressAfterEachScanEvent' --configuration-cache`: failed because `LibraryScanner.scan` had no `onProgress` parameter.
- Targeted: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerReportsProgressAfterEachScanEvent' --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- Scanner suite: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- Focused platform build: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `git diff --check`: pass (no output).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
- `progress.md`
Next owner: user for manual Android scan progress validation on a large SAF folder.
Blockers: none for automated JVM/desktop/Android verification. Live Android UI validation was not run in this session.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-09 scan cancel button state

Route: systematic-debugging
Owner: implementation
Input: User report: "Fix: It seems like clicking cancel does not take effect, at least on Android, iOS, macOS unknown"
Root cause:
- The scan cancel buttons in Library home and Settings called `scanJob?.cancel()` directly.
- The running scan coroutine used `scanner.scan(source) { scanJob?.isActive != true }` as a cooperative cancellation predicate.
- Cancelling the coroutine while `LibraryScanner.scan()` was iterating platform scanner sequences could surface as coroutine cancellation instead of a controlled `ScanStatus.Cancelled` session update, so the UI had no immediate state change and could appear to ignore the click.
Fix:
- Added an explicit remembered cancellation flag owned by `App()` and passed a single `onCancelScan` callback through Library home and Settings.
- Cancel now sets the flag for `LibraryScanner`'s cooperative cancellation path and immediately changes active UI progress from `Scanning` to `Cancelling`, preserving current counters until the scanner records the terminal `Cancelled` session.
- Added `AppScanCancellationTest` regression coverage for immediate active-scan state transition and terminal-session no-op behavior.
Verification:
- RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`: failed with unresolved `requestScanCancellation`, proving the regression test targeted missing behavior.
- Targeted: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- Existing scanner regression: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- Focused platform build: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `git diff --check`: pass (no output).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryHomeContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/AppScanCancellationTest.kt`
- `progress.md`
Next owner: user for manual Android scan-cancel validation on a large folder; implementation if the platform scan still takes too long to observe cancellation between deep tree events.
Blockers: none for automated JVM/desktop/Android verification. Live Android/iOS/macOS UI validation was not run in this session.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-09 Android release R8 Koin startup stack overflow

Route: systematic-debugging
Owner: implementation
Input: User report: Android debug build starts, release build crashes immediately with `java.lang.StackOverflowError`; retraced stack repeats through `org.koin.core.scope.Scope.resolve`, `SingleInstanceFactory.get`, and `RhythHausDiKt.rhythHausModule$lambda$0$5`.
Root cause:
- The release mapping retraced the loop to Koin resolving `PlatformAudioScanner` from a provider that called `get<PlatformSourceAccess>() as PlatformAudioScanner`.
- Under R8/Kotlin class-reference minification, that interface-alias binding triggered recursive Koin resolution/name lookup during app startup. Debug avoided the R8 path.
Fix:
- Made `PlatformSourceAccess` extend `PlatformAudioScanner`, so the platform source-access object is the scanner contract directly.
- Removed the separate Koin `single<PlatformAudioScanner>` alias and inject `PlatformSourceAccess` directly into `LibraryScanner` as its `PlatformAudioScanner` dependency.
- Removed redundant platform class/test double dual-interface declarations.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :androidApp:assembleRelease --configuration-cache`: pass (`BUILD SUCCESSFUL in 39s`; release R8/minify executed via `:androidApp:minifyReleaseWithR8`; existing Android artwork deprecation warning only, plus existing AAPT2 `SWIFT_DEBUG_INFORMATION_*` environment-variable warnings).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 13s`; existing `IOSNowPlayingBridgingTest` warnings and `SWIFT_DEBUG_INFORMATION_*` warnings only).
- `git diff --check`: pass (no output).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.ios.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`
- `progress.md`
Next owner: user for installing/running the generated release APK on the target Android device, since no adb device was connected in this session.
Blockers: automated release build/minification passed; live release app-start validation was not run because `adb devices` showed no connected devices.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-08 Coil artwork loading and Koin DI

Route: openspec+superpowers / subagent-driven-development
Owner: implementation
Input: User request: "Spec, plan, subagent-driven development: 1. Use coil to cache, generate thumbnail of the project's images including album art image. 2. Add koin for dependency injection, and use it in the exisiting code base"
Output:
- Added OpenSpec change `coil-koin-image-di` and Superpowers spec/plan artifacts for Coil-backed artwork loading and Koin dependency injection.
- Added Coil 3.5.0 and Koin 4.2.2 via the version catalog and shared dependencies.
- Added shared `rhythHausModule()` and idempotent `startRhythHausKoin()`; Android, desktop, and iOS entry points initialize Koin before rendering shared Compose UI.
- Refactored `App()` to resolve the existing service graph from Koin while preserving playback-controller disposal, folder picker scan flow, repository refresh, clear-library, and theme selection behavior.
- Added `ArtworkImage` with stable role/byte-size cache keys and Coil memory/disk cache policies; routed track row, compact NowPlayingBar, album card, artist row, drill-down top bar, and expanded NowPlayingScreen artwork through Coil while preserving existing fallbacks, content descriptions, shapes, content scale, selected overlays, gestures, and controls.
- Fixed review-discovered fallback regression by using `SubcomposeAsyncImage` error slot so corrupt/unsupported non-null artwork renders the existing fallback UI; drill-down top bar intentionally keeps empty fallback/no placeholder.
- Fixed final Android compile blocker by using Coil common `LocalPlatformContext.current` instead of JVM-only `coil3.PlatformContext.INSTANCE`.
Verification:
- `openspec validate coil-koin-image-di --strict`: pass (`Change 'coil-koin-image-di' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.di.RhythHausDiTest' --configuration-cache`: pass after transient Maven Central TLS retry (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ui.ArtworkImageTest' --configuration-cache`: pass after fallback/context fixes (`BUILD SUCCESSFUL in 3s`).
- Initial `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: failed in `:shared:compileAndroidMain` because `coil3.PlatformContext.INSTANCE` was unavailable for Android; fixed by switching to `LocalPlatformContext.current`.
- Final `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 44s`; 99 actionable tasks: 24 executed, 75 up-to-date; existing Android `MediaMetadata.Builder.setArtworkData` deprecation warning only).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass after Coil context fix (`BUILD SUCCESSFUL in 15s`; 34 actionable tasks: 8 executed, 26 up-to-date; existing `IOSNowPlayingBridgingTest` warnings only).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/di/RhythHausDi.kt`
- `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt`
- `desktopApp/src/main/kotlin/com/eterocell/rhythhaus/main.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/MainViewController.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/di/RhythHausDiTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/ArtworkImage.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ui/ArtworkImageTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/nowplaying/NowPlayingScreen.kt`
- `docs/superpowers/specs/2026-07-08-coil-koin-image-di-design.md`
- `docs/superpowers/plans/2026-07-08-coil-koin-image-di.md`
- `openspec/changes/coil-koin-image-di/proposal.md`
- `openspec/changes/coil-koin-image-di/design.md`
- `openspec/changes/coil-koin-image-di/specs/library-ui/spec.md`
- `openspec/changes/coil-koin-image-di/tasks.md`
- `progress.md`
- `roadmap.md`
Next owner: user for manual artwork-loading/cache visual QA on target device/simulator.
Blockers: none for automated OpenSpec/JVM/desktop/Android/iOS verification. Manual cache/performance/visual validation remains.
Commit: included in final semantic commit after staged diff review.


## Handoff - 2026-07-08 drill-down top-bar artwork

Route: openspec+superpowers / subagent-driven-development / planned scope correction
Owner: implementation
Input: User request: "Spec, plan, subagent-driven development: Show album art image in album/artis track list top bar"; corrections: image should fill the top bar, back button should have a circular background, album/artist name should have Material-chip-like backgrounds, blur should be removed when artwork owns the top bar, and the unscrolled state should be a full-width square top bar that compacts into a rectangular top bar while scrolling.
Output:
- Added OpenSpec change `drilldown-topbar-artwork` and Superpowers spec/plan artifacts for showing representative artwork in album/artist drill-down track-list top bars.
- Album and artist detail routes now derive ordered non-null `artworkBytes` candidates from their grouped tracks and pass them through `DrillDownView`.
- `DrillDownMiuixScrollChrome` now tries optional top-bar artwork candidates via `decodeArtworkCached()` until the first decodable image, then renders it as a full-width square top bar that height-animates into a compact rectangular top bar while scrolling, with a readability scrim, circular back-button background, and chip-style expanded/collapsed title overlays.
- No placeholder is rendered when artwork is absent or all candidates fail to decode; that fallback keeps the existing glass title top bar. When artwork is present, the drill-down top bar intentionally removes the blur/glass effect so the image can own the bar: full-width square top bar before scroll, compact rectangular top bar after scroll. Existing nested scroll, track rows, and Now Playing behavior remain scoped unchanged.
Verification:
- `openspec validate drilldown-topbar-artwork --strict`: pass (`Change 'drilldown-topbar-artwork' is valid`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`; 16 actionable tasks: 4 executed, 12 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`; 25 actionable tasks: 13 executed, 12 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass from previous same-scope verification (`BUILD SUCCESSFUL in 3s`; 99 actionable tasks: 12 executed, 87 up-to-date; existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`).
- `/usr/bin/xcrun xcodebuild -version`: pass from previous same-scope verification (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass from previous same-scope verification (`BUILD SUCCESSFUL in 12s`; 34 actionable tasks: 8 executed, 26 up-to-date; existing iOS test warnings only in `IOSNowPlayingBridgingTest` plus link warnings for unknown `SWIFT_DEBUG_INFORMATION_*` environment variables).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRoutes.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- `docs/superpowers/specs/2026-07-08-drilldown-topbar-artwork-design.md`
- `docs/superpowers/plans/2026-07-08-drilldown-topbar-artwork.md`
- `openspec/changes/drilldown-topbar-artwork/proposal.md`
- `openspec/changes/drilldown-topbar-artwork/design.md`
- `openspec/changes/drilldown-topbar-artwork/specs/library-ui/spec.md`
- `openspec/changes/drilldown-topbar-artwork/tasks.md`
- `progress.md`
- `roadmap.md`
Next owner: user for manual visual QA of full-width square-to-rectangle top-bar compaction animation, chip readability, and touch target feel on target devices/simulator.
Blockers: none for focused automated verification. Manual visual validation remains.
Commit: pending staged diff review.

## Handoff - 2026-07-07 Adopt MiuixScrollBehavior for drill-down track list

Route: openspec+superpowers / planned scope correction / systematic debugging
Owner: implementation
Input: User correction: "Still not right, I want to adopt MiuixScrollBehavior"
Output:
- Confirmed Miuix 0.9.3 exposes `MiuixScrollBehavior()` returning `ScrollBehavior`, and Miuix `TopAppBar` supports `title`, `largeTitle`, and `scrollBehavior`; `SmallTopAppBar` pins the behavior and is not sufficient for large-title collapse.
- Replaced the drill-down track-list chrome path with direct Miuix `TopAppBar(title = title, largeTitle = title, scrollBehavior = scrollBehavior, ...)` in RhythHaus glass.
- Attached `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` to the drill-down `LazyColumn`.
- Removed the separate expanded `DrillDownHeader`; the Miuix top app bar now owns both expanded large-title and collapsed title states with the back action present.
- Preserved Library home `NestedScrollBlurChrome`, list scroll reporting, route transitions, track rows, and Now Playing behavior.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`; 16 actionable tasks: 3 executed, 13 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`; 25 actionable tasks: 6 executed, 19 up-to-date; configuration cache reused).
- `openspec validate miuix-nested-scroll-top-app-bar --strict`: pass (`Change 'miuix-nested-scroll-top-app-bar' is valid`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 6s`; 99 actionable tasks: 11 executed, 88 up-to-date; configuration cache reused). Existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 14s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest`.
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `openspec/changes/miuix-nested-scroll-top-app-bar/design.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/specs/library-ui/spec.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
- `docs/superpowers/specs/2026-07-07-miuix-nested-scroll-top-app-bar-design.md`
- `docs/superpowers/plans/2026-07-07-miuix-nested-scroll-top-app-bar.md`
- `progress.md`
- `roadmap.md`
Next owner: user for visual QA on device/simulator; implementation owner if top padding/title position needs tuning.
Blockers: manual visual QA still needed to tune top padding/title position if Miuix layout height differs from expected 128.dp reservation.

## Handoff - 2026-07-07 Miuix nested-scroll drill-down correction

Route: openspec+superpowers correction
Owner: implementation
Input: User clarified Track list nested-scroll behavior: the expanded title/album name should move toward the collapsed top-bar title; the back button should also appear in the collapsed top bar; the expanded-state top-bar title such as `xx 首曲目` / artist subtitle should be removed.
Output:
- Removed the expanded-state drill-down subtitle top-bar title by rendering `DrillDownHeader` with an empty Miuix top-bar title and back action only.
- Passed `onBack` into `NestedScrollBlurChrome` from `DrillDownView`, so the collapsed Miuix top bar shows both the drill-down title and the back action after scrolling.
- Preserved the large drill-down title, `SectionLabel`, track rows, scroll reporting, left-edge swipe back, glass/backdrop shell, and Now Playing behavior.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 417ms`; 16 actionable tasks: 3 executed, 13 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 896ms`; 25 actionable tasks: 6 executed, 19 up-to-date; configuration cache reused).
- `openspec validate miuix-nested-scroll-top-app-bar --strict`: pass (`Change 'miuix-nested-scroll-top-app-bar' is valid`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryDetailContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `openspec/changes/miuix-nested-scroll-top-app-bar/specs/library-ui/spec.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
- `progress.md`
Next owner: user for visual QA of the drill-down title/back-button transition while scrolling.
Blockers: none for focused automated verification. Pre-existing modified `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` remains out of scope and was not touched.
Commit: pending staged diff review.

## Handoff - 2026-07-07 Miuix nested-scroll TopAppBar migration

Route: openspec+superpowers
Owner: implementation
Input: User asked to also migrate nested scroll to use the Miuix TopAppBar.
Output:
- Extended `RhythHausTopAppBar` with optional color, inset, and padding customization points while preserving existing defaults and Search/Settings/Library drill-down call-site compatibility.
- Migrated `NestedScrollBlurChrome` collapsed title content from a custom pulse-dot/title row to the Miuix TopAppBar path via `RhythHausTopAppBar(title = title, onBack = null, ...)`.
- Preserved RhythHaus glass/backdrop overlay, status-bar coverage, scroll progress threshold, bottom divider, Library scroll reporting, route transitions, bottom-bar visibility, and Now Playing behavior.
Verification:
- `openspec validate miuix-nested-scroll-top-app-bar --strict`: pass (`Change 'miuix-nested-scroll-top-app-bar' is valid`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 13 executed, 5 from cache, 81 up-to-date; configuration cache reused). Existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 11s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest`.
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryChrome.kt`
- `docs/superpowers/specs/2026-07-07-miuix-nested-scroll-top-app-bar-design.md`
- `docs/superpowers/plans/2026-07-07-miuix-nested-scroll-top-app-bar.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/proposal.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/design.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/specs/library-ui/spec.md`
- `openspec/changes/miuix-nested-scroll-top-app-bar/tasks.md`
- `progress.md`
- `roadmap.md`
Next owner: user for manual visual/runtime QA of Library home and drill-down nested-scroll chrome on target devices.
Blockers: none for automated verification. Pre-existing modified `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` remains out of scope and was not touched.
Commit: pending staged diff review.

## Handoff - 2026-07-07 Miuix TopAppBar migration

Route: openspec+superpowers
Owner: implementation
Input: User asked to use Miuix `TopAppBar` instead of the current custom top bar, with spec, plan, and subagent-driven development.
Output:
- Added shared `RhythHausTopAppBar`, a RhythHaus wrapper around Miuix `SmallTopAppBar` and Miuix `IconButton`, using existing Haus colors, localized back content description, and caller-owned insets.
- Replaced Search and Settings custom `BackChip` + title rows with `RhythHausTopAppBar`, preserving Search field/filter/result behavior and Settings scaffold/dropdown/scan controls.
- Replaced Library drill-down `BackChip` + subtitle row with `RhythHausTopAppBar(title = subtitle, onBack = onBack)`, preserving the large drill-down title, nested-scroll chrome, track rows, left-edge swipe back, and Now Playing bar behavior.
Verification:
- `openspec validate miuix-top-app-bar --strict`: pass (`Change 'miuix-top-app-bar' is valid`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 14 executed, 85 up-to-date; configuration cache reused). Existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 12s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest`.
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ui/RhythHausTopAppBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `docs/superpowers/specs/2026-07-07-miuix-top-app-bar-design.md`
- `docs/superpowers/plans/2026-07-07-miuix-top-app-bar.md`
- `openspec/changes/miuix-top-app-bar/proposal.md`
- `openspec/changes/miuix-top-app-bar/design.md`
- `openspec/changes/miuix-top-app-bar/specs/library-ui/spec.md`
- `openspec/changes/miuix-top-app-bar/tasks.md`
- `progress.md`
- `roadmap.md`
Next owner: user for manual visual/runtime QA of Search, Settings, and Library drill-down top bars on target devices.
Blockers: none for automated OpenSpec/JVM/desktop/Android/iOS verification. Pre-existing modified `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` remains out of scope and was not touched.
Commit: pending staged diff review.

## Handoff - 2026-07-07 Miuix component migration

Route: openspec+superpowers
Owner: implementation
Input: User asked to use `rhythhaus-miuix-usage` and migrate the project UI toward Miuix components, replacing bare custom components where suitable.
Output:
- Added `miuix-preference` at the existing Miuix `0.9.3` version line and wired `libs.miuix.preference` into shared commonMain.
- Migrated Settings appearance selection from a hand-rolled dropdown to Miuix `OverlayDropdownPreference` inside a local Miuix `Scaffold` popup host.
- Migrated Search input from custom `BasicTextField`/clear chip to Miuix `TextField`/`IconButton`, preserving the old stable muted border affordance with an outer 1dp border container.
- Audited Library rows, album cards, artwork/equalizer pieces, and Clear Library dialog. Kept product-specific rows/artwork/equalizer/route overlay custom; confirmed existing clear-library inner content already uses Miuix `Card`, `Button`, and `Text`.
Verification:
- `openspec validate miuix-component-migration --strict`: pass (`Change 'miuix-component-migration' is valid`).
- Initial `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: failed once in `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun passed, matching prior transient playback-test behavior.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- Rerun `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`; `99 actionable tasks: 7 executed, 92 up-to-date`).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 18s`; `43 actionable tasks: 23 executed, 20 up-to-date`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/search/SearchScreen.kt`
- `docs/superpowers/specs/2026-07-07-miuix-component-migration-design.md`
- `docs/superpowers/plans/2026-07-07-miuix-component-migration.md`
- `openspec/changes/miuix-component-migration/proposal.md`
- `openspec/changes/miuix-component-migration/design.md`
- `openspec/changes/miuix-component-migration/specs/library-ui/spec.md`
- `openspec/changes/miuix-component-migration/tasks.md`
- `progress.md`
- `roadmap.md`
Next owner: user for manual visual/runtime validation of Settings dropdown overlay and Search field affordance on target devices.
Blockers: none for automated OpenSpec/JVM/desktop/Android/iOS verification. Pre-existing modified `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` remains out of scope and was not touched by this migration.
Commit: pending staged diff review.

## Handoff - 2026-07-07 iOS locked-screen track-end completion

Route: systematic-debugging
Owner: implementation
Input: User report that on iOS, with the screen locked, music may stop when one track ends even though later tracks remain in the playlist; user selected the Swift-native helper option.
Root cause:
- The old iOS engine owned `AVAudioPlayer` in Kotlin/Native and inferred track completion from a 250 ms polling coroutine.
- At the exact moment a track ends under screen lock/background playback, audio stops and iOS can suspend/throttle the app before the next Kotlin polling tick, so `PlaybackController` may never receive `onPlaybackCompleted()` and therefore never advances the queue.
Fix:
- Added a Swift-owned `RhythHausAudioPlayerProvider` implementing `AVAudioPlayerDelegate.audioPlayerDidFinishPlaying` and registered it through a new KMP `IOSAudioPlayerBridge`.
- Reworked `PlaybackEngine.ios.kt` to use the Swift provider for load/play/pause/stop/seek/progress and to advance from the event-based completion callback instead of polling for end-of-track.
- Kept polling only for while-playing progress updates; track-end completion no longer depends on the progress loop.
- Added iOS regression coverage for bridge callback retention/forwarding and the chosen Swift delegate backend.
Verification:
- RED: `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache` failed before implementation with unresolved `IOSAudioPlayerCompletionHandler`, `IOSAudioPlayerProvider`, `IOSAudioBackend`, and `iosAudioBackend`.
- Targeted: `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 753ms`; 34 actionable tasks: 4 executed, 30 up-to-date; configuration cache reused).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- Swift/Xcode: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`: pass (`** BUILD SUCCEEDED **`). Initial run exposed Swift export naming (`play_()`), fixed by matching the generated KMP protocol name.
- Full platform subset: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 12s`; 133 actionable tasks: 13 executed, 120 up-to-date). Existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`.
- `git diff --check`: pass (no whitespace errors).
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridge.kt`: KMP Swift provider bridge and backend marker.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: delegates actual iOS audio operations to Swift provider and uses event-based completion.
- `iosApp/iosApp/iOSApp.swift`: Swift `AVAudioPlayerDelegate` provider and registration alongside existing artwork bridge.
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSAudioPlayerBridgeTest.kt`: regression tests for bridge callback forwarding and backend selection.
Next owner: user for on-device locked-screen validation across multiple consecutive tracks.
Blockers: none for automated KMP/iOS simulator/Xcode build validation; physical locked-screen playback behavior still requires manual device/simulator runtime validation.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-07 iOS Swift module split

Route: systematic-debugging follow-up refactor
Owner: implementation
Input: User asked to review the Swift code and separate modules into different files/packages after the Swift audio helper fix.
Output:
- Reviewed `iosApp/iosApp/iOSApp.swift` after the audio-helper implementation. It had unrelated responsibilities in one file: SwiftUI app entry, AVAudioPlayer provider, artwork bridge, Documents marker setup, AVAudioSession setup, and KMP bridge registration.
- Split Swift code into filesystem-synchronized Xcode source folders without editing `project.pbxproj`:
  - `iosApp/iosApp/App/RhythHausAppBootstrapper.swift`: one-time Documents container, AVAudioSession, remote-control-event, and KMP bridge setup.
  - `iosApp/iosApp/Audio/RhythHausAudioPlayerProvider.swift`: Swift-owned `AVAudioPlayerDelegate` provider for KMP completion callbacks.
  - `iosApp/iosApp/NowPlaying/RhythHausArtworkProvider.swift`: lockscreen / Control Center artwork bridge.
  - `iosApp/iosApp/iOSApp.swift`: now only owns provider lifetimes and SwiftUI scene startup.
Verification:
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`: pass (`** BUILD SUCCEEDED **`). Logs confirmed Xcode auto-discovered and compiled `App/RhythHausAppBootstrapper.swift`, `Audio/RhythHausAudioPlayerProvider.swift`, and `NowPlaying/RhythHausArtworkProvider.swift` via `PBXFileSystemSynchronizedRootGroup`.
- `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSAudioPlayerBridgeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`; 34 actionable tasks: 5 executed, 29 up-to-date).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`; 124 actionable tasks: 7 executed, 117 up-to-date; configuration cache reused).
- `git diff --check`: pass (no whitespace errors).
Next owner: user for on-device locked-screen multi-track validation.
Blockers: none for automated verification.
Commit: skipped; user did not ask to commit.

## Handoff - 2026-07-07 adaptive now playing screen

Route: openspec+superpowers
Owner: implementation
Input: adaptive-now-playing-screen spec/plan
Output: compact-preserving adaptive Now Playing split layout
Verification:
- `openspec validate adaptive-now-playing-screen --strict`: pass (`Change 'adaptive-now-playing-screen' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 322ms`; 25 actionable tasks: 4 executed, 21 up-to-date; configuration cache entry reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 13 executed, 5 from cache, 81 up-to-date; configuration cache entry reused). Existing warning only: `PlaybackEngine.android.kt:252:17 'fun setArtworkData(p0: ByteArray?): MediaMetadata.Builder' is deprecated`.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 22s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache entry reused). Existing warnings only in `IOSNowPlayingBridgingTest.kt` about unnecessary non-null assertions/no casts needed.
- `git diff --check`: pass (no output, exit 0).
- `grep -R "miuix-navigation3-adaptive\|ListDetailPaneScaffold\|androidx.navigation3.adaptive" -n gradle shared/src androidApp/src || true`: pass (no output).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt` (implementation commit `35b44ac`)
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt` (implementation commit `35b44ac`)
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt` (implementation commits `e5f825e`, `9ac2af4`)
- `docs/superpowers/specs/2026-07-07-adaptive-now-playing-screen-design.md` (tracked into final evidence commit)
- `docs/superpowers/plans/2026-07-07-adaptive-now-playing-screen.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-now-playing-screen/proposal.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-now-playing-screen/design.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-now-playing-screen/specs/now-playing-ui/spec.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-now-playing-screen/tasks.md`
- `progress.md`
- `.superpowers/sdd/task-4-report.md`
Next owner: user for manual wide/compact visual validation
Blockers: none for automated OpenSpec/JVM/desktop/Android/iOS verification. Manual visual validation remains.
Commit: final evidence commit (current HEAD after commit).

## Handoff - 2026-07-07 adaptive layout miuix blur final verification

Route: openspec+superpowers
Owner: implementation
Input: adaptive-layout-miuix-blur spec/plan
Output: adaptive wide layout + Miuix blur replacement evidence recorded in OpenSpec tasks and progress handoff
Verification:
- Initial `openspec validate adaptive-layout-miuix-blur --strict`: pass (`Change 'adaptive-layout-miuix-blur' is valid`).
- Initial `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 524ms`; 25 actionable tasks: 4 executed, 1 from cache, 20 up-to-date; configuration cache reused).
- Initial `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: failed (`BUILD FAILED in 10s`). Blockers: `:androidApp:checkDebugDuplicateClasses` reported duplicate `top.yukonga.miuix.kmp.*` classes from `miuix-ui-android:0.9.2` and transitive `miuix-android:0.8.5`; `:androidApp:processDebugMainManifest` reported `miuix-blur-android:0.9.2` minSdk 33 while app minSdk is 29.
- User-directed blocker fix: updated Miuix UI/blur to `0.9.3`; removed `miuix-navigation3-adaptive` completely; replaced the wide `ListDetailPaneScaffold` with a local two-pane Row shell; added Android manifest `tools:overrideLibrary="top.yukonga.miuix.kmp.blur"`; gated backdrop recording with `isRenderEffectSupported()` and blur use with `isRuntimeShaderSupported()`.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 21s`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 48s`).
- Final `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`; 108 actionable tasks: 6 executed, 102 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 34s`; 43 actionable tasks: 27 executed, 16 up-to-date; configuration cache entry stored).
- `git diff --check`: pass (no output, exit 0).
- `grep -R "com.kyant.backdrop\|kyant-backdrop\|kyant-shapes\|miuix-navigation3-adaptive\|ListDetailPaneScaffold" -n gradle shared/src androidApp/src || true`: pass (no output).
Changed files:
- `androidApp/src/main/AndroidManifest.xml`
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
- `openspec/changes/adaptive-layout-miuix-blur/tasks.md`
- `progress.md`
- `docs/superpowers/specs/2026-07-06-adaptive-layout-miuix-blur-design.md` (tracked into final evidence commit)
- `docs/superpowers/plans/2026-07-06-adaptive-layout-miuix-blur.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-layout-miuix-blur/proposal.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-layout-miuix-blur/design.md` (tracked into final evidence commit)
- `openspec/changes/adaptive-layout-miuix-blur/specs/library-ui/spec.md` (tracked into final evidence commit)
Next owner: user for manual tablet/desktop visual validation of the local two-pane layout and Android API <33 fallback visual validation.
Blockers: none for automated JVM/desktop/Android/iOS verification. Manual visual validation remains.
Commit: pending.

## Handoff - 2026-07-06 unify app glass tint

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User confirmed top and bottom Backdrop effect should use the same surface color/tint policy after identifying that background color differences could explain the mismatch.
Root cause:
- Top chrome used `HausColors.current.paper.copy(alpha = RhythHausGlassSurfaceAlpha)` while the bottom bar used `HausColors.current.panel.copy(alpha = RhythHausGlassSurfaceAlpha)`.
- Top chrome also drew an additional full-size gradient overlay using `panelStrong`, `panel`, and transparent colors, while bottom bar did not.
- Blur/refraction constants were unified, but the visible glass tint was still different.
Fix:
- Switched top chrome fallback color to `HausColors.current.panel.copy(alpha = RhythHausGlassSurfaceAlpha)` to match bottom bar.
- Removed the top chrome's extra full-size gradient/tint overlay.
- Left title row and bottom divider layout unchanged.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for visual confirmation that top and bottom glass now match.
Blockers: none for compile/test/build verification. Visual matching remains runtime/manual.
Commit: pending.

## Handoff - 2026-07-06 unify app glass blur

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported the top bar and bottom bar Backdrop alpha/blur radius did not match and asked to unify the Backdrop effect blur across the app.
Root cause:
- `NestedScrollBlurChrome` passed progress-dependent Backdrop params: fallback alpha `0.34f + 0.42f * progress`, blur `(6 + 10 * progress).dp`, refraction height `(8 + 8 * progress).dp`, and refraction amount `(12 + 12 * progress).dp`.
- `NowPlayingBar` passed fixed Backdrop params: alpha `0.72f`, blur `10.dp`, refraction height `16.dp`, and refraction amount `24.dp`.
- The shared wrapper default blur was still `8.dp`, so there was no single app-level glass definition.
Fix:
- Added shared constants in `LiquidGlassChrome.kt`: `RhythHausGlassSurfaceAlpha`, `RhythHausGlassBlurRadius`, `RhythHausGlassRefractionHeight`, and `RhythHausGlassRefractionAmount`.
- Updated `rhythHausLiquidGlass(...)` defaults to use those constants.
- Removed custom blur/refraction params from top chrome and bottom bar call sites.
- Updated top chrome and bottom bar fallback alpha to use `RhythHausGlassSurfaceAlpha`.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `progress.md`
Next owner: user for Android/iOS visual confirmation that top and bottom glass strength now match.
Blockers: none for compile/test/build verification. Visual tuning remains runtime/manual.
Commit: pending.

## Handoff - 2026-07-06 fix duplicate bottom bar layer

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User attached screenshot showing the NowPlayingBar rendering as two visible panel layers and asked to fix the two layers in the bottom bar.
Root cause:
- `NowPlayingBar` applied `rhythHausLiquidGlass(...)` to `barModifier`, then wrapped the content in a Miuix `Surface` with the same rounded shape and shadow.
- Even with transparent color, the Surface still contributed its own shaped/elevated container behavior, creating a nested/duplicate rounded panel on top of the glass card.
Fix:
- Replaced the outer `Surface` wrapper with a plain `Box(modifier = barModifier)`.
- Removed the unused Miuix `Surface` import.
- Kept the content column, progress bar, artwork, text, play/search/settings controls, click and swipe modifiers unchanged.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `progress.md`
Next owner: user for visual confirmation that the bottom bar now renders as a single glass panel.
Blockers: none for compile/test verification. Visual layering requires runtime confirmation.
Commit: pending.

## Handoff - 2026-07-06 fix nested chrome status-bar seam

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported a separate visible line between the nested-scroll top bar and the status bar, with the Backdrop effect not continuous between them.
Root cause:
- `NestedScrollBlurChrome` applied Backdrop glass to the full status-bar + toolbar height, but its tint/gradient overlay was drawn only inside the bottom 56dp toolbar sub-box.
- The status-bar part and toolbar part therefore had different overlay treatment even though they shared a Backdrop modifier, creating a visible seam at the boundary.
Fix:
- Moved the chrome gradient/tint overlay from the toolbar-only child to a full-size `matchParentSize()` child of the glass chrome container.
- Kept the title row and bottom divider inside the toolbar-height child so text/divider layout is unchanged.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for Android/iOS visual confirmation that the status-bar/top-bar seam is gone.
Blockers: none for compile/test verification. Visual continuity requires runtime confirmation.
Commit: pending.

## Handoff - 2026-07-06 fix nested chrome status-bar backdrop sampling

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported the status-bar background remained solid even after full-size backdrop recording; the Backdrop effect still did not visually cover the status-bar area.
Root cause:
- The recorded full-size backdrop included the status-bar area, but the list itself was still moved down with outer `Modifier.padding(top = statusBarHeight)`, so the pixels under the status bar were still only solid paper.
- The nested chrome therefore sampled/drew against solid paper in the status-bar strip even though the chrome box height included the status bar.
Fix:
- Changed Home and drill-down lists from outer top padding to `LazyColumn(contentPadding = PaddingValues(top = statusBarHeight))` so the list viewport/layer spans behind the status bar while the first row remains offset below it.
- Added `rememberSystemBarTopPadding()` to use the max of `WindowInsets.statusBars` and `WindowInsets.systemBars` top padding for more robust Android/iOS top inset sizing.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 9s`).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for Android/iOS visual confirmation of status-bar glass coverage.
Blockers: none for compile/test/build verification. Visual status-bar coverage requires runtime confirmation.
Commit: pending.

## Handoff - 2026-07-06 fix Backdrop status-bar coverage

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported the nested-scroll top bar and Backdrop effect did not cover the status bar on both iOS and Android after the RenderThread crash fix.
Root cause:
- The previous crash fix moved `recordRhythHausBackdrop(...)` from route-level surfaces to each `LazyColumn` after `Modifier.padding(top = statusBarHeight)`.
- That prevented self-recording crashes, but it also made the recorded backdrop begin below the status bar, so `NestedScrollBlurChrome` could draw a full-height box while its Backdrop sample/fallback coverage appeared to start below the status bar.
Fix:
- Wrapped Home route content in a non-glass `Box(...recordRhythHausBackdrop(homeBackdrop))` that includes the paper background and status-bar area.
- Moved `NestedScrollBlurChrome` outside that recorded Home subtree so the chrome draws from, but is not recorded into, the same backdrop.
- Applied the same pattern to drill-down routes: record a full-size non-glass content box containing the paper surface/list, then draw scrollbar/chrome/bottom bar as overlay siblings outside the recorded subtree.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 4s`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for Android/iOS visual confirmation that the glass top chrome now covers the status-bar area without reintroducing the RenderThread crash.
Blockers: none for compile/test/build verification. Visual status-bar coverage requires runtime confirmation.
Commit: pending.

## Handoff - 2026-07-06 fix Backdrop RenderThread crash

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported Android RenderThread SIGSEGV after the liquid-glass Backdrop change and linked Kyant Backdrop's Glass Bottom Sheet tutorial describing the same crash pattern.
Root cause:
- Backdrop's docs identify `Fatal signal 11 (SIGSEGV), code 2 (SEGV_ACCERR)` as a self-referential draw loop when `layerBackdrop` records content that later draws from the same backdrop.
- RhythHaus recorded `homeBackdrop` on the whole Home `Surface` and `drillDownBackdrop` on the whole drill-down `Surface`. Both surfaces contained glass overlays (`NestedScrollBlurChrome`, and on drill-down also `NowPlayingBar`) that draw from the same recorded backdrop, creating the same loop on Android RenderThread.
Fix:
- Moved `recordRhythHausBackdrop(homeBackdrop)` from the Home route `Surface` to the Home `LazyColumn` content only.
- Moved `recordRhythHausBackdrop(drillDownBackdrop)` from the drill-down route `Surface` to the drill-down `LazyColumn` content only.
- Kept the glass top chrome and bottom bars outside their same-backdrop recording scope so they sample content but are not recorded into the backdrop they draw.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 1s`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`).
- `git diff --check`: pass (no output, exit 0).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for Android runtime repro confirmation that the RenderThread SIGSEGV no longer occurs.
Blockers: none for compile/test/build verification. Runtime crash cannot be fully proven fixed without running the Android app through the reported screen path.
Commit: pending.

## Handoff - 2026-07-06 liquid glass backdrop chrome

Route: openspec+superpowers
Owner: implementation
Scope: Replace nested-scroll top chrome and bottom NowPlayingBar panel surfaces with Kyant0 Backdrop liquid-glass effect.
Implementation:
- Added Backdrop and Shapes dependencies through the version catalog.
- Added local `LiquidGlassChrome.kt` wrapper for Backdrop recording/effects/fallback draw surface.
- Recorded Library/Home and drill-down content layers and routed nested-scroll top chrome through Backdrop glass.
- Routed root and drill-down `NowPlayingBar` rounded card containers through Backdrop glass while preserving existing controls and gestures.
Verification:
- `openspec validate liquid-glass-backdrop-chrome --strict`: pass (`Change 'liquid-glass-backdrop-chrome' is valid`).
- `git diff --check`: pass (no output, exit 0).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 36s`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 26s`).
Acceptance:
- Requirement matched: yes — visible Library/Home chrome, drill-down chrome, and bottom bar rounded card now use Kyant0 Backdrop glass with fallback surface draw.
- Scope controlled: yes — no playback, scanner, navigation model, route-transition, empty-library, or control redesign changes beyond backdrop recording and glass surfaces.
- Edge cases/risk reviewed: Backdrop shader support may vary by platform; fallback surface remains readable; manual visual validation recommended on Android/iOS/macOS for final glass tuning.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LiquidGlassChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `openspec/changes/liquid-glass-backdrop-chrome/proposal.md`
- `openspec/changes/liquid-glass-backdrop-chrome/design.md`
- `openspec/changes/liquid-glass-backdrop-chrome/tasks.md`
- `openspec/changes/liquid-glass-backdrop-chrome/specs/library-ui/spec.md`
- `docs/superpowers/specs/2026-07-06-liquid-glass-backdrop-chrome-design.md`
- `docs/superpowers/plans/2026-07-06-liquid-glass-backdrop-chrome.md`
- `progress.md`
Next owner: user for manual visual validation of glass appearance on Android/iOS/macOS.
Blockers: none for automated verification.
Commit: implementation commits `a29a6cd`, `bd3c6d5`, `12802cc`, `acc0df6`; final docs/evidence commit pending.


## Handoff - 2026-07-06 fix nested scroll chrome layout

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported nested scroll chrome issues: on the main Library screen the scrolled top bar covered the whole screen with the title at the bottom; on track-list screens the chrome worked but its color did not cover the iOS status bar.
Root cause:
- The main screen placed `LazyColumn` and `NestedScrollBlurChrome` directly inside `Surface` (Miuix `Box` with `propagateMinConstraints = true`). `Surface` propagated full-screen min constraints to the chrome, and `Modifier.height(chromeHeight)` only constrained max height, allowing the chrome to stretch to the full screen on the main screen.
- The track list wrapped its content in an inner `Box` (default `propagateMinConstraints = false`), which is why it did not stretch, but `NestedScrollBlurChrome` read `WindowInsets.statusBars` internally. If the `LazyColumn`'s `statusBarsPadding()` consumed the insets before the chrome read them, the chrome could compute a height that did not include the status bar.
Fix:
- Changed `NestedScrollBlurChrome` to accept `statusBarHeight: Dp` as a parameter and use `Modifier.requiredHeight(chromeHeight)` so its height is exact regardless of propagated min constraints.
- Read the status bar height once in each route (Library/Home and `DrillDownView`) before the list consumes insets, replaced `LazyColumn.statusBarsPadding()` with `Modifier.padding(top = statusBarHeight)`, and passed the same height to `NestedScrollBlurChrome`.
- Wrapped the main screen `LazyColumn` + chrome in an inner `Box` to match the working track-list pattern and isolate layout constraints.
Verification:
- `./gradlew :shared:compileKotlinJvm :desktopApp:compileKotlin --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`; one known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` failed on first broad run, passed on targeted rerun).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- iOS simulator visual check (forced chrome visible): main screen chrome now sits at the top only, title is at the top, and the scrim extends behind the status bar; at rest the chrome is hidden as intended.
- `git diff --check`: pass (no output).
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for on-device visual confirmation, especially track-list scroll behavior on iOS.
Blockers: none.

## Handoff - 2026-07-06 fix iOS TagLib deployment target mismatch

Route: systematic-debugging (bugfix)
Owner: implementation
Input: Xcode Release/device link warning: `Shared.framework/Shared[149](rh_taglib.cpp.o)` was built for newer iOS 26.5 than being linked at iOS 26.0.
Root cause:
- `taglib/build.gradle.kts` built iOS TagLib static archives with custom CMake tasks and no explicit `CMAKE_OSX_DEPLOYMENT_TARGET`, so AppleClang used the current iPhoneOS 26.5 SDK default/min version for object metadata while `iosApp` links at `IPHONEOS_DEPLOYMENT_TARGET = 26.0`.
- A second issue made the build nondeterministic across iOS targets: both `iosArm64` and `iosSimulatorArm64` copied `librhythhaus_taglib.a` / `libtag.a` to the same `src/nativeInterop/cinterop/` paths, so device and simulator builds could overwrite each other's archives.
Fix:
- Added `rhythhaus.ios.deploymentTarget=26.0` in `gradle.properties`; `taglib/build.gradle.kts` resolves `iosTagLibDeploymentTarget` from Xcode's `IPHONEOS_DEPLOYMENT_TARGET` environment variable when present, otherwise falls back to the Gradle property, and passes it to CMake as `-DCMAKE_OSX_DEPLOYMENT_TARGET` for iOS TagLib builds.
- Moved generated iOS TagLib archives to target-specific build outputs under `taglib/build/generated/iosTagLib/<target>/`, and the generated cinterop `.def` now points each KMP target at its own library directory.
Verification:
- Rebuilt clean iOS TagLib/device framework path: `./gradlew :shared:linkReleaseFrameworkIosArm64 --configuration-cache` -> `BUILD SUCCESSFUL`.
- Verified deployment target source selection: default CLI build wrote `CMAKE_OSX_DEPLOYMENT_TARGET=26.0`; test override `IPHONEOS_DEPLOYMENT_TARGET=26.1 ./gradlew :taglib:buildIosTagLibHelperIosArm64 --configuration-cache` wrote `CMAKE_OSX_DEPLOYMENT_TARGET=26.1`; rebuilt default artifacts back to 26.0 afterward.
- Inspected rebuilt object metadata: `rh_taglib.cpp.o` reports `minos 26.0`, `sdk 26.5`.
- Xcode Release/device build: `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -destination 'generic/platform=iOS' CODE_SIGNING_ALLOWED=NO build` -> `** BUILD SUCCEEDED **`; grep count for `was built for newer 'iOS' version` was `0`.
- Simulator coverage after target-specific archive separation: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> `BUILD SUCCESSFUL`.
Changed files:
- `gradle.properties`: `rhythhaus.ios.deploymentTarget=26.0` fallback/source of truth for CLI builds.
- `taglib/build.gradle.kts`: Xcode env/Gradle property provider chain for iOS native deployment target and target-specific generated static archive paths.
Next owner: user for normal Xcode Archive/signing validation if desired.
Blockers: none.

## Handoff - 2026-07-03 navigation animation polish

Route: openspec+superpowers
Owner: implementation
Scope: Three polish items on top of the base navigation-animations change: (1) Android predictive back visual progress, (2) NowPlayingBar fixed during route transitions, (3) NowPlayingBar expands/collapses into Now Playing with a growth animation instead of a route-push fade/slide.
Implementation:
- Predictive back: read `navState.transitionState` (kept `NavigationEventInfo.None`; confirmed `NavigationEventInfo.Slide` does not exist in `navigationevent` 1.1.x by extracting the sources jar). When `transitionState` is `InProgress` with direction `TRANSITIONING_BACK`, extract `latestEvent.progress` (0f-1f) and apply it as a horizontal offset on the `AnimatedContent` route container so the current route visually tracks the drag. Gesture completion still pops via the existing `onBackCompleted`; cancellation naturally resets to 0 because progress is derived fresh from `transitionState` every recomposition, not stored.
- Fixed bottom bar: restructured `LibraryHomeScreen` root into a `Box` containing `AnimatedContent` (route content, now `fillMaxSize()` + predictive-back offset) and `NowPlayingBar` as a fixed sibling aligned `BottomCenter`. Removed the previous `NowPlayingBar` call from inside the Home/Settings/Search/ClearLibraryDialog route branch.
- Bottom bar expand/collapse: replaced the `LibraryRoute.NowPlaying` route-push rendering (which used to show `NowPlayingScreen` via the standard `AnimatedContent` fade/slide) with an empty placeholder inside `AnimatedContent`, and added a new `NowPlayingExpandOverlay` composable rendered outside `AnimatedContent`, driven by an `Animatable<Float>` (0f = bar, 1f = full screen; expand `tween(300)`, collapse `tween(250)`). The overlay grows a `Surface` from the bottom via `fillMaxHeight(fraction)`, shrinks its top corner radius from 24dp to 0dp as it grows, and fades the `NowPlayingScreen` content in via `alpha(fraction)`. `leftEdgeSwipeBack` and `BackChip` inside `NowPlayingScreen` are unchanged for closing.
Self-review (independent reviewer subagent timed out after 600s with 0 findings returned; coordinator performed the review directly):
- Confirmed no double-render: the `NowPlaying` branch inside `AnimatedContent` is an empty `Box`; actual content only renders in `NowPlayingExpandOverlay`.
- Confirmed `predictiveBackProgress` is safe: computed fresh from `navState.transitionState` each recomposition (not `remember`ed), so it naturally returns to 0 when `transitionState` returns to `Idle` after gesture completion or cancellation.
- Confirmed `LaunchedEffect(isVisible)` cannot race: `Animatable.animateTo` cancels any in-flight animation on the same instance before starting a new one, so rapid open/close taps are safe.
- Open caveat not fully resolved by automated checks: the `LibraryRoute.NowPlaying` target still participates in `AnimatedContent`'s route transition (transitioning to/from an empty `Box`), so the underlying screen still runs its own fade/slide via `routeContentTransform` while the expand overlay grows on top of it simultaneously. This is expected to look fine since the overlay covers the screen as it grows, but has not been visually confirmed on a device/simulator.
Verification:
- `openspec validate navigation-animation-polish --strict`: pass (`Change 'navigation-animation-polish' is valid`).
- `openspec validate navigation-animations --strict`: pass (`Change 'navigation-animations' is valid`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 98 actionable tasks: 13 executed, 5 from cache, 80 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 22s`; 33 actionable tasks: 8 executed, 25 up-to-date).
- `git diff --check`: pass (no output, exit 0).
Acceptance:
- Requirement matched: yes for all three polish items at the source/automated-verification level; predictive back progress, fixed bottom bar, and expand/collapse animation are implemented per spec.
- Scope controlled: yes — no new dependencies, no `SharedTransitionScope` adoption, no native navigation migration, no changes to playback, scanner, library persistence, theme, or Now Playing screen content layout beyond the expand wrapper.
- Edge cases/risk reviewed: the external reviewer subagent timed out without returning a verdict; coordinator performed the review directly and found no blocking issues, but flagged the simultaneous-transition-layering caveat above as needing manual visual confirmation on device/simulator (Android 13+ for predictive back gesture in particular).
Changed files:
- `docs/superpowers/specs/2026-07-02-navigation-animation-polish-design.md`
- `docs/superpowers/plans/2026-07-02-navigation-animation-polish.md`
- `openspec/changes/navigation-animation-polish/proposal.md`
- `openspec/changes/navigation-animation-polish/design.md`
- `openspec/changes/navigation-animation-polish/specs/library-navigation/spec.md`
- `openspec/changes/navigation-animation-polish/tasks.md`
- `openspec/changes/navigation-animation-polish/.openspec.yaml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `progress.md`
Next owner: user for manual visual validation, especially Android 13+ predictive back gesture and the bar-to-Now-Playing expand/collapse feel on device; OpenSpec/user for archive of both `navigation-animations` and `navigation-animation-polish` when satisfied.
Blockers: none for automated verification. Manual visual confirmation not performed in this session.
Commit: pending — user asked for the base navigation-animations work and this polish work to land in one commit; awaiting staged-diff review and approval.

## Handoff - 2026-07-02 navigation animations

Route: openspec+superpowers
Owner: implementation
Scope: Add shared Compose direction-aware route transition animations for Home, detail, Now Playing, Search, Settings, and Clear Library dialog routes.
Implementation:
- Added pure navigation transition classification for push/pop/replace/root/no-op route changes.
- Added common tests covering transition classification and preserving route-stack behavior.
- Wrapped shared route rendering in root-level AnimatedContent with direction-aware push/pop/root/replace transitions.
- Replaced the Clear Library platform `Dialog` with an in-window Compose overlay so that route also participates in the AnimatedContent transition.
- Preserved existing visible back, left-edge swipe, Android system/predictive back, playback, scanner, library, and theme behavior.
Verification:
- `openspec validate navigation-animations --strict`: pass (`Change 'navigation-animations' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass before final fix (`BUILD SUCCESSFUL in 357ms`) and pass after final fix (`BUILD SUCCESSFUL in 329ms`; 24 actionable tasks: 4 executed, 20 up-to-date; configuration cache reused).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass after final fix (`BUILD SUCCESSFUL in 323ms`; 15 actionable tasks: 3 executed, 12 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial fail in known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` (`60 tests completed, 1 failed`; Android debug package still completed before `:shared:jvmTest` failure); targeted rerun passed; exact broad rerun passed; final post-fix broad run passed (`BUILD SUCCESSFUL in 5s`; 98 actionable tasks: 12 executed, 86 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --configuration-cache`: pass (`BUILD SUCCESSFUL in 919ms`; 33 actionable tasks: 5 executed, 28 up-to-date).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass before final fix (`BUILD SUCCESSFUL in 21s`) and pass after final fix (`BUILD SUCCESSFUL in 13s`; 33 actionable tasks: 8 executed, 25 up-to-date; configuration cache reused).
- `git diff --check`: pass (no output, exit 0).
- `git diff --stat`: reviewed; tracked diff summary after final fix/evidence updates showed `progress.md`, `App.kt`, `LibraryNavigation.kt`, and `LibraryNavigationTest.kt` with 341 insertions and 149 deletions; untracked new OpenSpec/docs/report files are listed below.
Reviews:
- Task 1 review: clean.
- Task 2 review: clean after timeout recovery.
- Final whole-change review: found one Important issue that Clear Library platform `Dialog` would likely not animate inside parent `AnimatedContent`.
- Final fix and re-review: clean; no Critical, Important, or Minor findings.
Acceptance:
- Requirement matched: yes — shared route changes animate, push/pop directions are distinct, and Clear Library uses in-window content instead of platform `Dialog` so it participates in the route transition.
- Scope controlled: yes — no new dependencies, native navigation migration, playback, scanner, persistence, or theme behavior changes.
- Edge cases/risk reviewed: automated checks prove route metadata and compilation; subjective animation polish still needs manual visual validation on Android/iOS/macOS.
Changed files:
- `docs/superpowers/specs/2026-07-02-navigation-animations-design.md`
- `docs/superpowers/plans/2026-07-02-navigation-animations.md`
- `openspec/changes/navigation-animations/proposal.md`
- `openspec/changes/navigation-animations/design.md`
- `openspec/changes/navigation-animations/specs/library-navigation/spec.md`
- `openspec/changes/navigation-animations/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `.superpowers/sdd/navigation-animations/task-3-report.md`
- `progress.md`
Next owner: user for manual visual validation; OpenSpec/user for archive when satisfied.
Blockers: none for automated verification. One broad verification run hit known flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun and exact broad rerun passed.
Commit: pending semantic commit after user reviews staged diff, unless user asks not to commit.

## Handoff - 2026-07-02 fix iOS lockscreen controls and artwork regression

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported iOS lockscreen media controls are again greyed out and album art does not show.
Investigation evidence:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` still ignored the handler tokens returned by `MPRemoteCommand.addTargetWithHandler`, matching the known Kotlin/Native MediaRemote failure mode where enabled commands can be dropped by iOS and rendered as unsupported/greyed.
- Several remote handlers called `updateNowPlayingInfo()` without explicit paused/stopped `playbackRate = 0.0`, leaving stale `MPNowPlayingInfoPropertyPlaybackRate = 1.0` after pause/stop/toggle-pause paths.
- Kotlin replaced the whole `MPNowPlayingInfoCenter.nowPlayingInfo` dictionary on every refresh without carrying forward Swift-inserted `MPMediaItemPropertyArtwork`, so artwork set by `RhythHausArtworkProvider` was deleted by later play/pause/seek/progress updates.
Fix:
- Retained all iOS remote command handler tokens for the engine lifetime in `remoteCommandHandlerTokens`.
- Made pause/toggle-pause/stop remote command handlers publish explicit `playbackRate = 0.0`; seek now mirrors the actual player playing state.
- Preserved any existing `MPMediaItemPropertyArtwork` entry when rebuilding the Now Playing dictionary, and reset `artworkTrackId` during track-switch teardown so the next track republishes artwork.
- Added `nowPlayingDictionaryPreservesArtworkAndExplicitPausedRate` iosTest coverage.
Verification:
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 13s`); `TEST-iosSimulatorArm64Test.com.eterocell.rhythhaus.IOSNowPlayingInfoTest.xml` shows 5 tests, 0 failures, including `nowPlayingDictionaryPreservesArtworkAndExplicitPausedRate`.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `git diff --check`: pass.
Acceptance:
- Requirement matched: source-level regression causes for greyed command routing and artwork deletion are addressed.
- Scope controlled: yes — iOS playback engine/test only; no Android, desktop, scanner, database, dependency, or UI behavior changed.
- Not verified: live lockscreen/Control Center visual confirmation on simulator/device was not captured in this CLI session.
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`
Next owner: user for live iOS lockscreen/Control Center visual confirmation with an artwork-bearing track.
Blockers: none for automated validation; no live iOS visual capture was performed.

## Handoff - 2026-07-01 Android release metadata R8 keep rules

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported metadata scans correctly in Android debug builds but not in release builds.
Root cause: Release builds enable R8 minification/shrinking while debug builds do not. Source/build inspection showed the Android TagLib JNI bridge returns `NativeTagLibReadResult` from native C++ via `FindClass`/constructor signatures. A release APK dex inspection before the fix showed R8 kept `NativeTagLibBridge` native method names but optimized `NativeTagLibReadResult` into an abstract shell with no constructor, fields, or accessors. That makes the native result mapping fail only in minified release builds, so metadata falls back to display-name/default values. Debug APKs kept the normal Kotlin data-class shape.
Fix:
- Added `androidApp/proguard-rules.pro` with keep rules for JNI native method descriptor classes and the TagLib JNI bridge/result/write metadata classes.
- Wired the release build type in `androidApp/build.gradle.kts` to use the default optimized Android rules plus `proguard-rules.pro`.
Tight feedback loop:
- Before fix, `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease --configuration-cache` built both variants; release dex check failed because `NativeTagLibReadResult` had `Access flags 0x0401 (PUBLIC ABSTRACT)` and no constructor/accessors.
- After fix, release dex check passes: `NativeTagLibReadResult` is `PUBLIC FINAL`; `NativeTagLibBridge.readFdNative`/`readPathNative`, `NativeWriteBridge.writePathNative`, and `WriteMeta` getters are present.
Verification:
- `./gradlew :androidApp:assembleRelease --rerun-tasks --configuration-cache`: pass (`BUILD SUCCESSFUL`); followed by dex inspection keep-check pass.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug :androidApp:assembleRelease --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass.
Acceptance:
- Requirement matched: yes for the release-vs-debug metadata cause that is reproducible locally; the release APK now preserves the JNI result classes needed by metadata scanning.
- Scope controlled: yes — Android release shrinker configuration only; no scanner logic, database schema, playback, native TagLib C++, or metadata merge behavior changed.
- Edge cases/risk reviewed: live Android SAF/provider scanning in an installed release build still needs device validation with real tagged files; automated verification proves the release-only R8/JNI break is fixed.
Changed files:
- `androidApp/build.gradle.kts`: release build now applies default optimized ProGuard rules plus app rules.
- `androidApp/proguard-rules.pro`: keep rules for TagLib JNI bridges/result/value classes.
Next owner: user for installing release build and rescanning/clearing existing fallback rows if needed.
Blockers: none for automated validation; no Android device was attached for live release scan validation.

## Handoff - 2026-07-01 Android SAF metadata fallback

Route: systematic-debugging (bugfix follow-up)
Owner: implementation
Input: User reported that after the FD TagLib change, scanned Android tracks still have missing metadata and duration `0:00`.
Investigation evidence:
- Current Android APK contains `librhythhaus_taglib.so` for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Native symbol check confirmed all packaged/generated Android `.so` slices export `Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readFdNative`, `Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative`, and `rh_taglib_read_fd`.
- A local native probe using the macOS helper verified `rh_taglib_read_fd` can read the generated WAV fixture title/artist/album/duration from a normal seekable FD, so the FD bridge itself works for at least file-backed descriptors.
- No adb device was attached (`adb devices` listed none), so exact Android provider/runtime failure could not be captured from device logs.
Root-cause hypothesis acted on:
- Real Android SAF providers can still produce descriptors/streams that TagLib cannot fully parse or cannot derive duration from. Because `AudioMetadataReader` returned null on TagLib Unsupported/Failed or accepted partial TagLib results with null duration, the scanner persisted fallback title/artist/album and null duration, rendering as `0:00`.
Fix:
- Added a platform metadata fallback seam: `internal expect fun readPlatformAudioMetadata(source: AudioSource): AudioMetadata?`.
- Android actual uses `MediaMetadataRetriever` for `AudioSource.FileDescriptor` via `/proc/self/fd/<fd>` and for `AudioSource.Uri` via `setDataSource(context, uri)`.
- `AudioMetadataReader` now merges missing fields from the platform fallback when TagLib returns no metadata or partial metadata lacking title/artist/album/duration/artwork. TagLib remains the primary reader; Android framework metadata fills gaps, especially duration.
- JVM/iOS actual fallbacks return null, preserving current TagLib-only behavior there.
- Added regression coverage where TagLib returns title/artist/album but no duration; scanner fills duration from the platform fallback while preserving TagLib fields.
Verification:
- Native probe: `rh_taglib_read_path` and `rh_taglib_read_fd` both returned status 0, title/artist/album, duration=1, sample=8000, channels=1 for `/tmp/rhythhaus-fd-fixture.wav`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerFillsMissingDurationFromPlatformMetadataFallback' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass.
Changed files added in this follow-up:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.ios.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
Next owner: user for Android device clear/re-scan/manual validation with real SAF music files. If metadata still shows fallback values, capture device logcat around scan; no device was connected in this session.
Blockers: none for automated validation; live Android SAF provider behavior not device-verified.
Commit: not created; user did not ask to commit.

## Handoff - 2026-07-01 Android SAF FD metadata

Route: systematic-debugging (bugfix)
Owner: implementation
Scope: Replace Android SAF metadata temp-file handoff with a file-descriptor handoff so TagLib can read metadata without copying user audio into app storage.
Root cause: Android SAF playback sources are `content://` URIs, while RhythHaus TagLib metadata reads previously only accepted filesystem paths. The prior temp-file fix avoided persistent copies, but still copied every document before TagLib could run; if the metadata source fell back to URI, `AudioMetadataReader` returned null and tracks kept fallback metadata.
Fix:
- Added a metadata-only `AudioSource.FileDescriptor(fd, displayName)` and routed `AudioMetadataReader` through a new `TagLibReader.readFd` path.
- Added native `rh_taglib_read_fd(int fd, const char* display_name)` using `dup(fd)` plus TagLib `FileStream`/`FileRef(IOStream*)`, with shared extraction logic for path and descriptor reads.
- Added Android JNI `readFdNative(fd, displayName)` and wired `AndroidNativeTagLibReader.readFd` to it.
- Changed Android SAF scanning to open each supported document with `ContentResolver.openFileDescriptor(document.uri, "r")`, pass that descriptor as the metadata source, and close it in `cleanupMetadataAudioSource` after scanner enrichment. Playback persistence still uses the original `content://` URI.
- Kept legacy persistent metadata cache removal on scan start so old copied-cache files are cleaned on rescan.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`); rebuilt Android TagLib helper slices during the build.
- `./gradlew :taglib:buildAllAndroidTagLibHelpers --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :taglib:buildMacosTagLibHelper :taglib:buildAllAndroidTagLibHelpers --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — Android SAF metadata now uses option 2 (file descriptor) instead of temp-copy files; original `content://` playback URI is still persisted.
- Scope controlled: yes — no database schema, playback behavior, scanner UI, or iOS/JVM metadata source changes beyond exhaustive `AudioSource` handling.
- Edge cases/risk reviewed: `FileDescriptor` is metadata-only and throws if accidentally routed to platform playback. Automated tests/builds prove compilation and scanner routing; real Android provider behavior still needs manual device rescan with local music to confirm provider seekability and actual embedded metadata/artwork extraction.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioMetadata.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/SqlDelightLibraryRepository.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
- `taglib/src/commonMain/kotlin/com/eterocell/rhythhaus/taglib/RhythHausTagLib.kt`
- `taglib/src/androidMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.android.kt`
- `taglib/native/include/rh_taglib.h`
- `taglib/native/src/rh_taglib.cpp`
- `taglib/native/jni/rh_taglib_jni.cpp`
Next owner: user for Android device rescan/manual validation with real SAF music files; implementation if a provider returns a non-seekable descriptor and needs a fallback.
Blockers: none for automated validation.
Commit: not created; user did not ask to commit.

## Handoff - 2026-07-01 fix Android SAF import metadata copies

Route: systematic-debugging (bugfix)
Owner: implementation
Scope: Fix Android folder import behavior where scanned SAF audio files appeared copied into app storage and metadata fallback values returned again.
Root cause: Android SAF scanning intentionally preserved playback as `content://` URIs, but copied every supported document into a persistent app cache under `cacheDir/rhythhaus-taglib/<sourceId>` so native TagLib could read a filesystem path. That made imports look like the app had copied the music into internal storage. Metadata fallback happened whenever that filesystem handoff failed or fell back to the original URI, because `AudioMetadataReader` returns null for `AudioSource.Uri`.
Fix:
- Changed Android SAF metadata handoff to create a per-candidate temporary file under `cacheDir/rhythhaus-taglib-temp`, pass that filesystem path only to metadata extraction, and persist the original `content://` playback URI unchanged.
- Added `cleanupMetadataAudioSource` to `AudioScanCandidate` and `audioCandidateForSourceFile`; `LibraryScanner` now always invokes it in a `finally` after metadata read, so the temp copy is deleted whether TagLib succeeds or falls back.
- Removed the legacy persistent metadata-cache directory for the scanned source at scan start so old internal-storage copies are cleaned up on the next Android rescan.
- Extended the existing scanner regression to assert that the metadata filesystem source is used while the playback URI is preserved and cleanup runs.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — Android persisted tracks still reference the selected external SAF document URI; copied files are now metadata-only temporary files cleaned after each read, with legacy persistent cache cleaned on rescan.
- Scope controlled: yes — no scanner UI, database schema, playback engine, native TagLib ABI, iOS, or JVM folder behavior changes.
- Manual note: existing rows with fallback metadata may need a rescan to refresh stored title/artist/album/duration/artwork.
Changed files:
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryModels.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`
Next owner: user for Android device rescan/manual validation with real local music files.
Blockers: none for automated validation.

## Handoff - 2026-07-01 fix iOS lockscreen skip buttons greyed out

Route: systematic-debugging (bugfix)
Owner: implementation
Scope: Fix user report — iOS lockscreen media controls, previous/next (forward/backward) buttons greyed out.
Root cause (Phase 1/2): `IOSPlaybackEngine.registerRemoteCommands()` enabled and handled
`previousTrackCommand`/`nextTrackCommand`, but never explicitly disabled the skip-interval commands
(`skipForwardCommand`, `skipBackwardCommand`, `seekForwardCommand`, `seekBackwardCommand`). iOS
prefers the interval commands over the track commands on the lock screen when the interval commands
are left enabled; with no handler attached, they render greyed out and suppress the working
previous/next track buttons.
Fix (Phase 4, single change):
- Extracted the command enable/disable wiring from `registerRemoteCommands()` into a new
  internal top-level `configureIOSRemoteCommandAvailability(commandCenter: MPRemoteCommandCenter)`
  in `PlaybackEngine.ios.kt`, called from `registerRemoteCommands()` unchanged (still guarded by
  `remoteCommandsRegistered` to avoid handler accumulation, per known cinterop pitfall).
  The function now additionally disables `skipForwardCommand`, `skipBackwardCommand`,
  `seekForwardCommand`, `seekBackwardCommand`.
- Added `remoteCommandConfigurationEnablesTrackControlsAndDisablesIntervalControls` regression test
  in `IOSNowPlayingInfoTest.kt` (iosTest) asserting the real `MPRemoteCommandCenter.sharedCommandCenter()`
  singleton's `.enabled` flags after calling the new function — not a mock.
Verification:
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 16s`);
  new test confirmed executed via `TEST-iosSimulatorArm64Test.com.eterocell.rhythhaus.IOSNowPlayingInfoTest.xml`
  (4 tests, 0 failures, including `remoteCommandConfigurationEnablesTrackControlsAndDisablesIntervalControls`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
Acceptance:
- Requirement matched: yes — root cause fixed at the source (command registration), not a UI-level workaround.
- Scope controlled: yes — iOS-only change, no touch to Android/desktop media session code, no new
  dependencies, no behavior change to play/pause/stop/scrub/track-skip handlers themselves.
- Not verified: actual on-device/simulator lock-screen visual confirmation that the buttons are no
  longer greyed. The fix is verified at the API level (command `.enabled` state) which directly
  controls the lock-screen rendering per Apple's `MPRemoteCommandCenter` behavior, but no screenshot
  evidence was captured.
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`
Next owner: user for on-device/simulator visual confirmation; no further automated action pending.
Blockers: none for automated validation; on-device lock-screen visual confirmation not performed.

## Handoff - 2026-07-01 track art thumbnails

Route: openspec+superpowers
Owner: implementation
Scope: Add memory-only cached thumbnail artwork decode path for compact track-list and now-playing-bar artwork.
Root cause: Source inspection showed compact track-row artwork (`AlbumMark` inside `TrackRow`) and `NowPlayingBar` decoded full embedded artwork bytes in Compose surfaces used during list scrolling. Lazy-list row composition could therefore trigger full-size image decode work for a 54dp row mark.
Implementation:
- Added cache-key separation for full-size and thumbnail artwork entries in `ArtworkCache`.
- Added `decodeArtworkThumbnail(maxPixelSize: Int)` expect/actual implementations: Android uses sampled `BitmapFactory` decode plus final scaling; JVM/iOS use Skia raster thumbnail rendering.
- Added `decodeArtworkThumbnailCached(maxPixelSize: Int = 128)` for compact UI surfaces.
- Added `ArtworkCacheTest` coverage for cache bucket separation, empty-cache behavior, and rectangular thumbnail dimension bounds without requiring Skiko native image construction in tests.
- Routed `AlbumMark`/`TrackRow` and compact `NowPlayingBar` through `decodeArtworkThumbnailCached()`.
- Preserved original `artworkBytes` on track/playback models and kept expanded `NowPlayingScreen` on full-size `decodeArtwork()`.
Verification:
- `openspec validate track-art-thumbnails --strict`: pass (`Change 'track-art-thumbnails' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.ArtworkCacheTest' --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:compileKotlinJvm :shared:compileAndroidMain --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `rg 'decodeArtwork\\(' shared/src/commonMain/kotlin/com/eterocell/rhythhaus shared/src/androidMain/kotlin/com/eterocell/rhythhaus shared/src/jvmMain/kotlin/com/eterocell/rhythhaus shared/src/iosMain/kotlin/com/eterocell/rhythhaus`: pass; direct full decode remains in platform actuals, `NowPlayingScreen`, legacy `NowPlayingCard`, and common cached full-size helper.
- `rg 'decodeArtworkThumbnailCached' shared/src/commonMain/kotlin/com/eterocell/rhythhaus`: pass; shows `AlbumMark`, `NowPlayingBar`, and helper.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — compact row/bar artwork uses cached thumbnails, expanded Now Playing remains full-size, and original artwork bytes remain available for platform metadata/full artwork.
- Scope controlled: yes — no dependency, SQLDelight schema, scanner, TagLib/native metadata, playback-engine, MediaSession, audio-session, or platform media metadata changes.
- Edge cases/risk reviewed: runtime scroll/FPS improvement still needs manual validation with a large artwork-heavy library; this change removes the source-level full-decode-on-row-composition hotspot.
Changed files:
- `docs/superpowers/plans/2026-07-01-track-art-thumbnails.md`
- `docs/superpowers/specs/2026-07-01-track-art-thumbnails-design.md`
- `openspec/changes/track-art-thumbnails/design.md`
- `openspec/changes/track-art-thumbnails/proposal.md`
- `openspec/changes/track-art-thumbnails/specs/library-ui/spec.md`
- `openspec/changes/track-art-thumbnails/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ArtworkDecoder.ios.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ArtworkCacheTest.kt`
- `progress.md`
Next owner: user for manual scroll-performance validation with a large artwork-heavy library; OpenSpec/user for archive when satisfied.
Blockers: none for automated verification.
Commit: `b139cfa docs: spec track artwork thumbnails`, `dab756f feat: add artwork thumbnail cache`, `e7491ef fix: use thumbnails for compact artwork`, `1ffb579 fix: tighten artwork thumbnail cache tests`, `f9ec7ab docs: record track artwork thumbnail evidence`.

## Handoff - 2026-07-01 ui ux fixes batch

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `ui-ux-fixes-batch`: empty-library onboarding/adaptive album grid, Songs browse mode, Search polish and 44dp compact controls, removal of user-facing TagLib developer panels, and final evidence handoff.
Implementation:
- Added OpenSpec change artifacts under `openspec/changes/ui-ux-fixes-batch/` and Superpowers design/plan documents under `docs/superpowers/`.
- Added common `LibraryBrowserTest` coverage for album-grid breakpoints and `BrowseMode` ordering.
- Added `albumGridColumnsForWidth(widthDp: Float)` and adaptive album-grid rendering; empty Home now shows `ImportAudioCard`.
- Extended browse mode to `Albums, Artists, Songs`; Songs mode renders all tracks and starts playback using the full-library playable queue.
- Added Search query `Clear` action and dismisses Search after a result starts playback.
- Increased BackChip and bottom-bar Search/Settings effective hit targets to at least 44dp while preserving icon sizes/callbacks.
- Removed normal UI TagLib developer panels and dead developer-only panel/helper code from App/NowPlayingScreen.
Verification:
- `openspec validate ui-ux-fixes-batch --strict`: pass (`Change 'ui-ux-fixes-batch' is valid`).
- Task-level focused checks passed during execution, including `:shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest'` and `:shared:compileKotlinJvm` where required.
- `rg 'DEV · TagLib|ALL PROPERTIES|URI source — TagLib requires a filesystem path' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: pass/no matches (`rg_exit=1`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 542ms`).
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 2m 43s`).
Acceptance:
- Requirement matched: yes — all five OpenSpec task groups have recorded evidence and task-scoped reviews for Tasks 1-4 found no critical or important issues.
- Scope controlled: yes — no new dependencies; no native platform UI rewrite; no scanner, metadata extraction, playback engine, MediaSession, audio-session, database schema, playlists, genres, folders/sources, recently-added, queue redesign, or stable album identity changes.
- Edge cases/risk reviewed: no live visual QA/device screenshot evidence was performed or claimed; manual cross-device visual validation remains optional.
Changed files:
- `docs/superpowers/plans/2026-07-01-ui-ux-fixes-batch.md`
- `docs/superpowers/specs/2026-07-01-ui-ux-fixes-batch-design.md`
- `openspec/changes/ui-ux-fixes-batch/design.md`
- `openspec/changes/ui-ux-fixes-batch/proposal.md`
- `openspec/changes/ui-ux-fixes-batch/specs/library-ui/spec.md`
- `openspec/changes/ui-ux-fixes-batch/tasks.md`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryBrowser.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryBrowserTest.kt`
Next owner: user/OpenSpec for optional manual visual validation and eventual archive of `ui-ux-fixes-batch` when satisfied.
Blockers: none for automated validation.
Commit:
- `defab2b docs: spec ui ux fixes batch`
- `793d3b8 docs: plan ui ux fixes batch`
- `82c0bc8 feat: improve empty library album browsing`
- `db87aed feat: add songs browse mode`
- `dc27d69 fix: polish search and compact controls`
- `21d5810 fix: remove user facing developer panels`
- final evidence update: `docs: record ui ux fixes batch evidence`

## Handoff - 2026-07-01 replace emoji controls with icons

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `replace-emoji-controls-with-icons`: replace transport/search/settings control glyphs with Material vector icons and record evidence.
Implementation:
- Added a shared commonMain Compose Material Icons Extended dependency alias and dependency. The planned `org.jetbrains.compose.material:material-icons-extended:1.11.1` artifact did not resolve, so the icon artifact is pinned to the available JetBrains Compose icon version `1.7.3` while existing Compose Multiplatform dependencies remain unchanged.
- Replaced `NowPlayingBar.kt` play/pause/empty play, search, and settings control `Text` glyphs with `Icon` using `PlayArrow`, `Pause`, `Search`, and `Settings` image vectors.
- Replaced `NowPlayingScreen.kt` previous/play-pause/next transport `Text` glyphs with `Icon` using `SkipPrevious`, `PlayArrow`/`Pause`, and `SkipNext` image vectors.
- Preserved existing control containers, sizes, theme-driven colors/tints, click behavior, playback behavior, navigation behavior, queue behavior, scanner, persistence, platform code, and non-control artwork fallback text.
Verification:
- `openspec validate replace-emoji-controls-with-icons --strict`: pass (`Change 'replace-emoji-controls-with-icons' is valid`).
- `rg '▶|⏸|⏮|⏭|🔍|⚙️' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: initial fail because `org.jetbrains.compose.material:material-icons-extended:1.11.1` was unavailable; pass after pinning icon artifact to `1.7.3` (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — targeted transport/search/settings controls now render vector icons with content descriptions and theme-aware tints instead of targeted emoji/text glyphs.
- Scope controlled: yes — no playback, queue, scanner, persistence, navigation, theme selection, platform-specific code, or out-of-scope artwork fallback changes.
- Edge cases/risk reviewed: automated compile/test verification passed; manual visual confirmation remains optional for icon appearance across devices.
Changed files:
- `gradle/libs.versions.toml`: Material Icons Extended alias/version.
- `shared/build.gradle.kts`: commonMain Material Icons dependency.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`: mini-player vector icons.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`: expanded transport vector icons.
- `openspec/changes/replace-emoji-controls-with-icons/tasks.md`: completed tasks and evidence.
- `.superpowers/sdd/replace-emoji-controls-with-icons-report.md`: implementation/verification report.
- `progress.md`: handoff evidence.
Next owner: OpenSpec/user for archive or manual visual validation if desired.
Blockers: none.
Commit: pending semantic commit `fix: replace emoji controls with vector icons`.

## Handoff - 2026-07-01 polish track row selected copy

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `polish-track-row-selected-copy`: selected `TrackRow` user-facing copy and evidence updates.
Implementation:
- Replaced selected `TrackRow` debug/prototype text `queued on shared UI ...%` with `Now playing`.
- Removed now-unused `selectionAlpha` animation state and `animateFloatAsState`/`tween` imports.
- Preserved selected-row highlight, row click behavior, metadata display, duration display, playback, queue semantics, scanner, persistence, navigation, theme selection, and platform-specific code.
Verification:
- `openspec validate polish-track-row-selected-copy --strict`: pass (`Change 'polish-track-row-selected-copy' is valid`).
- `rg 'queued on shared UI|selectionAlpha' shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: pass/no matches (exit 1, empty output).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`). Warnings were existing `PredictiveBackHandler` deprecation and expect/actual beta warnings.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`). Warnings were existing Android artwork deprecation, `PredictiveBackHandler` deprecation, and expect/actual beta warnings.
Acceptance:
- Requirement matched: yes — selected rows display `Now playing` and no longer expose debug text or selected-state percentages.
- Scope controlled: yes — implementation code change is isolated to `TrackRow`/imports in `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`; evidence files updated.
- Edge cases/risk reviewed: no behavior/state changes; manual visual smoke remains optional for copy appearance.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: selected-row copy and unused animation imports/state removal.
- `openspec/changes/polish-track-row-selected-copy/tasks.md`: completed tasks and evidence.
- `.superpowers/sdd/polish-track-row-selected-copy-report.md`: implementation/verification report.
- `progress.md`: handoff evidence.
Next owner: OpenSpec/user for archive or manual UI visual validation if desired.
Blockers: none.
Commit: `d3255b6 fix: polish selected track row copy`.

## Handoff - 2026-06-30 standardize back navigation

Route: openspec+superpowers
Owner: implementation
Scope: Implement OpenSpec change `standardize-back-navigation`: shared visible back chip, root predictive/system back route-pop handling, Android manifest opt-in, and handoff evidence.
Implementation:
- Added shared commonMain `BackChip` with visible label `‹ Back`, `Back` content description, rounded dark chip styling, and existing `hausClickable` feedback.
- Replaced drill-down `← BACK`, now-playing `← LIBRARY`, and search/settings `< Back` labels with `BackChip` while preserving existing route-pop callbacks.
- Replaced the `LibraryHomeScreen` root `BackHandler(enabled = navigation.canPop)` with `PredictiveBackHandler(enabled = navigation.canPop)` that pops one route after completed predictive/system back progress.
- Removed duplicate child `BackHandler` registrations for drill-down, now playing, settings, search, and clear-library dialog so root navigation owns system/predictive back consumption. Preserved shared left-edge swipe-back gestures for drill-down and now playing.
- Added `android:enableOnBackInvokedCallback="true"` to the Android main activity.
Verification:
- `openspec validate standardize-back-navigation --strict`: pass (`Change 'standardize-back-navigation' is valid`).
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`). Warning: `PredictiveBackHandler` is deprecated in Compose 1.11.1 in favor of `NavigationEventHandler`, expected/recorded in the plan.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial run failed in known/flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --configuration-cache`: pass.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion' --rerun-tasks --configuration-cache`: pass.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --rerun-tasks --configuration-cache`: pass (`BUILD SUCCESSFUL`).
Acceptance:
- Requirement matched: yes — all visible targeted back controls use shared `‹ Back`, route-pop callbacks are unchanged, and Android predictive/system back is wired at the route-stack owner with manifest opt-in.
- Scope controlled: yes — no playback, scanner, theme selection, library persistence, or route semantics changed.
- Edge cases/risk reviewed: Android predictive-back visual preview still needs manual Android 13+ emulator/device validation; automated verification proves wiring compiles and broad JVM/desktop/Android checks pass.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `androidApp/src/main/AndroidManifest.xml`
- `openspec/changes/standardize-back-navigation/tasks.md`
- `.superpowers/sdd/standardize-back-navigation-report.md`
- `progress.md`
Next owner: user for manual Android 13+ predictive-back gesture preview/runtime validation.
Blockers: none for automated verification.
Commit: semantic commit with message `feat: standardize back navigation`.

## Handoff - 2026-06-30 theme selection

Route: openspec+superpowers (subagent-driven with coordinator recovery after subagent timeout/stale reports)
Owner: implementation
Scope: Add persisted System/Light/Dark theme selection and light/dark shared Compose palettes using AndroidX DataStore Preferences.
Implementation:
- Added DataStore 1.2.1 dependencies and a shared `ThemePreferenceStore` with Android, iOS, and JVM/macOS actuals.
- Added `RhythHausThemeMode`, stable serialization/parsing, display labels/descriptions, light/dark Haus palettes, and palette resolution tests.
- Wired `App()` to collect the persisted theme mode, resolve System against platform dark-mode state, provide `LocalHausColors`, and choose Miuix light/dark color schemes.
- Migrated shared UI color usage to active palette accessors across App, Settings, Search, Now Playing, bottom bar, and scrubber surfaces.
- Added Settings Appearance section with System/Light/Dark options and persisted selection callback.
Verification:
- `openspec validate theme-selection --strict`: valid.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6 Build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL on final rerun. An earlier broad run failed once in known transient `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun of that test passed before the final broad rerun passed.
Acceptance:
- Requirement matched: yes — Settings exposes System/Light/Dark; selection is DataStore-backed and persisted across supported platforms; shared UI resolves light/dark palettes.
- Scope controlled: yes — no SQLDelight preference table, no native settings screens, no playback/scanner/library schema changes.
- Edge cases/risk reviewed: invalid persisted values fall back to System; manual visual validation is still recommended on Android/iOS/macOS for dark-theme aesthetics.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Theme.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.android.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.ios.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.jvm.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausColors.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicProgressScrubber.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/ThemeTest.kt`
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/ThemePreferenceStoreJvmTest.kt`
- `openspec/changes/theme-selection/*`
- `docs/superpowers/specs/2026-06-30-theme-selection-design.md`
- `docs/superpowers/plans/2026-06-30-theme-selection.md`
- `progress.md`
Next owner: user for manual visual validation of dark/light/system themes on devices.
Blockers: none for automated verification.

Update - dropdown theme selector:
- User requested replacing the three Appearance cards with a dropdown list.
- Design approved: one compact shared Compose dropdown row that expands to System, Light, and Dark choices and reuses the existing persisted selection callback.

## Handoff - 2026-06-30 explicit navigation stack

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Replace ad-hoc shared Compose navigation booleans/nullables with explicit route stack.
Implementation:
- Added `LibraryRoute` and `LibraryNavigationStack` with common tests.
- Refactored `LibraryHomeScreen` route rendering for Home, Album Detail, Artist Detail, Now Playing, Search, Settings, and Clear Library dialog.
- Removed top-level `showClearDialog`, `showSettings`, and `showSearch` state from `App()` and `LibraryHomeScreen` parameters.
- Converted bottom-bar and drill-down settings/search entry points to `LibraryRoute.Settings` / `LibraryRoute.Search` pushes.
- Converted Clear Library dialog visibility, dismiss/cancel/confirm, Settings clear-library action, Search dismiss, central Android `BackHandler`, and left-edge/back callbacks to route stack push/pop operations.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: BUILD SUCCESSFUL.
- `openspec validate explicit-navigation-stack --strict`: valid (`Change 'explicit-navigation-stack' is valid`).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: initial run failed once in pre-existing/flaky `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion`; targeted rerun of that test passed; reran full command and it was BUILD SUCCESSFUL. Warnings: Compose `BackHandler` deprecation in favor of NavigationEventHandler; existing expect/actual beta and Android artwork deprecation warnings remain.
Acceptance:
- Requirement matched: yes.
- Scope controlled: yes; no playback behavior changes intended, and no unrelated modified files were touched beyond required OpenSpec/progress evidence.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `openspec/changes/explicit-navigation-stack/tasks.md`
- `progress.md`
Next owner: user for manual Android system/gesture-back validation on device/emulator.
Blockers: none.

## Current state

Last updated: 2026-06-25
Current change: UI polish (button font, next-track sync) + iOS lockscreen player panel
Three-commit bugfix series on main: Clear Library font, NowPlayingScreen next-track staleness, iOS MPRemoteCommandCenter
Workflow route: openspec+superpowers
State source of truth: OpenSpec for durable product changes; Superpowers for clarification/brainstorming/task execution discipline; this file for session continuity and verification evidence.

## Handoff - 2026-06-30 bottom bar insets + Android back navigation

Route: systematic-debugging
Owner: implementation
Input: User reported bottom bar covering album/artist content, missing bottom inset padding when Android navigation bar is hidden, and Android back/swipe-back closing the app instead of returning to the previous in-app screen.
Root cause:
- Main and album/artist drill-down LazyColumns only left a fixed 8dp/80dp trailing spacer while NowPlayingBar is overlaid at the bottom, so final album/artist rows could scroll under the bar.
- NowPlayingBar used `safeContentPadding()`, which does not intentionally reserve a bottom gutter when Android is in gesture/hidden-navigation mode.
- Shared Compose navigation state was local (`selectedAlbum`, `selectedArtist`, `showNowPlayingScreen`, overlays), but no Compose back handler consumed Android system back gestures/buttons before the Activity default finished the app.
Output:
- `NowPlayingBar.kt`: bottom bar now uses `navigationBarsPadding()` plus an explicit 12dp bottom gutter and 16dp side inset; exported `NowPlayingBarContentPadding = 144.dp` for list content clearance.
- `App.kt`: main and drill-down lists now use the shared bottom content spacer; album/artist drill-down, settings/search overlays, and clear dialog register Compose `BackHandler` callbacks to pop/dismiss instead of exiting the app.
- `NowPlayingScreen.kt`: expanded now-playing screen registers `BackHandler(onBack)` so Android back returns to the prior screen.
- `gradle/libs.versions.toml` and `shared/build.gradle.kts`: added the Compose `ui-backhandler` dependency needed for shared back handling.
Verification:
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL. Warnings: Compose BackHandler is deprecated in favor of NavigationEventHandler; existing expect/actual and Android artwork deprecation warnings remain.
Acceptance:
- Requirement matched: yes for bottom list safe area, extra bottom bar inset in hidden-nav/gesture mode, and Android system back handling for in-app screens/overlays.
- Scope controlled: yes; only shared Compose layout/navigation and the dependency alias were changed.
Changed files:
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
Next owner: user for manual Android gesture-nav validation on device/emulator.
Blockers: none for compile/test verification.

## Handoff - 2026-06-30 Android hardware media-button controls (IMPLEMENTED, awaiting device validation)

Route: openspec+superpowers (durable playback-architecture change per AGENTS.md), executed via subagent-driven-development (reviewer subagent gate).
Owner: implementation -> awaiting user on-device validation before OpenSpec archive.
Input: User report "cable control is not working on Android just like airpods control wouldn't work on iOS previously." User approved the full MediaSessionService refactor.
Root cause (systematic-debugging Phase 1, refined by reading media3 1.10.1 source): the Android engine built a standalone Activity-scoped Media3 `MediaSession` (`PlaybackEngine.android.kt`) with no `MediaSessionService`, no audio focus, a single `setMediaItem` (so next/prev had nothing to act on), and no audio-becoming-noisy handling. media3 does self-register a runtime button receiver for an active session, so the dominant real gaps were missing audio focus + foreground service surface + queue + becoming-noisy.
Output:
- New `shared/src/androidMain/.../RhythHausPlaybackService.kt`: `MediaSessionService` hosting ExoPlayer with `AudioAttributes(usage=MEDIA, content=MUSIC, handleAudioFocus=true)` + `setHandleAudioBecomingNoisy(true)`; wraps player in `SkipRoutingPlayer` (ForwardingPlayer) advertising next/prev and routing them to the bridge. `onTaskRemoved` keeps service alive while playing (no super, canonical pattern).
- New `shared/src/androidMain/.../RhythHausTransportBridge.kt`: process-level @Volatile skip handlers so the service player drives the shared controller's queue (single source of truth; no forked playlist).
- `PlaybackEngine.android.kt`: restructured to connect via async `MediaController` (`SessionToken` + `buildAsync`); FIFO `pendingActions` queue (fixes load+play-before-connect race); `disposed` guard against connection-callback resurrection; `release()` cancels scope + tears down controller/future/bridge; listener setter wires bridge -> `onSkipToNext/onSkipToPrevious`.
- `androidApp/.../MainActivity.kt`: requests `POST_NOTIFICATIONS` at runtime on API 33+ (backs lock-screen/notification transport surface).
- `androidApp/src/main/AndroidManifest.xml`: declares the service (`foregroundServiceType=mediaPlayback`), `MediaButtonReceiver`, and `FOREGROUND_SERVICE`/`FOREGROUND_SERVICE_MEDIA_PLAYBACK`/`POST_NOTIFICATIONS` permissions.
- New `shared/src/androidHostTest/.../RhythHausTransportBridgeTest.kt`: 4 tests (handler invocation, no-op safety, engine listener->bridge wiring).
- OpenSpec change `openspec/changes/android-media-button-controls/` (proposal/design/tasks/specs); `openspec validate --strict` -> valid.
Reviewer subagent (spec+quality gate) found 2 Critical + 3 Important; all addressed: C1 single-slot pendingAction overwrite -> FIFO queue; C2 missing POST_NOTIFICATIONS runtime request -> added in MainActivity; I1 uncancelled scope -> scope.cancel() in release(); I2 release-during-connect leak -> disposed guard releases late-arriving controller; I3 double-release -> branch on controller!=null. M1 skip wrap-around is pre-existing PlaybackController behavior (unchanged).
Verification: `./gradlew :androidApp:assembleDebug` -> BUILD SUCCESSFUL; `:shared:jvmTest` + `:shared:testAndroidHostTest` -> BUILD SUCCESSFUL; merged debug manifest confirmed to contain the service (mediaPlayback), MediaButtonReceiver, and FOREGROUND_SERVICE_MEDIA_PLAYBACK. Not committed (pre-existing unrelated working-tree changes present; AGENTS.md commits only when fully complete).
Next owner: user for on-device validation matrix — wired cable inline-remote play/pause, Bluetooth play/pause, lock-screen/notification next/previous, unplug-mid-playback auto-pause, and POST_NOTIFICATIONS prompt on API 33+. Residual untested seam: async MediaController connection ordering (not device-independently testable; covered by the FIFO fix + disposed guard but needs runtime confirmation).
Blockers: cable/BT/lock-screen behavior cannot be validated in this environment (no physical device); pre-existing ktlint error in SwipeBackGesture.kt (untouched file) fails repo-wide spotlessApply.

## Handoff - 2026-06-30 Android hardware media-button controls (AWAITING APPROVAL)

Route: openspec+superpowers (durable playback-architecture change per AGENTS.md)
Owner: OpenSpec (proposal staged) -> awaiting user approval before implementation
Input: User report "cable control is not working on Android just like airpods control wouldn't work on iOS previously."
Root cause (systematic-debugging Phase 1): `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt:45` builds a standalone Media3 `MediaSession` in Activity-scoped code (`MainActivity` -> `setRhythHausAndroidContext`). The OS delivers hardware media buttons (wired cable inline remote, Bluetooth) as `android.intent.action.MEDIA_BUTTON` broadcasts, which require a registered `androidx.media3.session.MediaButtonReceiver` backed by a `MediaSessionService` to receive/route them. Verified absent: no `MediaSessionService`, no `MediaButtonReceiver`, no `<service>`/`<receiver>` in `androidApp/src/main/AndroidManifest.xml` (only the launcher activity). A bare Activity-scoped session has no delivery target, so the broadcasts are dropped. Android twin of the prior iOS AirPods bug (missing `MPRemoteCommandCenter` handlers). Secondary gaps: single `setMediaItem` (no queue -> next/prev have nothing to skip to) and no audio-becoming-noisy handling (no auto-pause on unplug).
Output: Staged OpenSpec change `openspec/changes/android-media-button-controls/` (proposal.md, design.md, tasks.md, specs/android-media-controls/spec.md ADDED, specs/audio-playback/spec.md MODIFIED). `openspec validate android-media-button-controls --strict` -> valid. NO code changed (durable playback-architecture work requires user-approved spec+plan per AGENTS.md; cable/BT controls need on-device manual validation unavailable here).
Verification: spec validation only; no build run for this change (no code touched).
Next owner: user to approve the proposal/plan, then implementation owner (subagent-driven-development) executes tasks.md; manual device matrix (cable remote, BT, lock-screen next/prev, unplug auto-pause) required before archive.
Blockers: awaiting user approval; manual on-device validation cannot run in this environment.

Note: also regenerated Android launcher icons from `icons/dark_mode.svg` this session (gradient adaptive foreground + 10 mipmap PNGs); `:androidApp:assembleDebug` BUILD SUCCESSFUL. Separate from the media-button change.

## Handoff - 2026-06-30 iOS archive version sync

Route: systematic-debugging
Owner: implementation
Input: User observed that running Archive in Xcode does not trigger `./gradlew syncIosVersionXcconfig`, leaving `iosApp/Configuration/Version.xcconfig` stale after editing root `gradle.properties`.
Root cause: `Version.xcconfig` is read while Xcode resolves build settings for archive, but the existing target build phase only runs `:shared:embedAndSignAppleFrameworkForXcode`; no archive/run pre-action invokes `syncIosVersionXcconfig` before build settings are evaluated. After commit `400bcbe` changed root version to 0.0.3, `Version.xcconfig` still contained 0.0.2 and `xcodebuild -showBuildSettings` resolved iOS version values to 0.0.2.
Output: Added an Xcode scheme pre-action that runs `./gradlew syncIosVersionXcconfig --configuration-cache` from the repo root with the same `JAVA_HOME`/Homebrew PATH setup used by the Kotlin framework build phase. Synced committed `Version.xcconfig` to 0.0.3. The local ignored user scheme was also updated so this developer's current Xcode scheme triggers the pre-action immediately.
Verification: A Python assertion comparing `gradle.properties` to `xcodebuild -showBuildSettings` failed before sync (`MARKETING_VERSION` stale: expected 0.0.3, actual 0.0.2). After adding the pre-action and intentionally resetting `Version.xcconfig` stale, `xcodebuild ... archive CODE_SIGNING_ALLOWED=NO` ran `:syncIosVersionXcconfig`, completed with `** ARCHIVE SUCCEEDED **`, rewrote `Version.xcconfig` to 0.0.3/000003, and the archive Info.plist reported CFBundleShortVersionString 0.0.3 and CFBundleVersion 000003.
Next owner: user for normal signed Xcode Archive/Organizer validation.
Blockers: none for unsigned archive verification.

## Handoff - 2026-06-30 Android TagLib SAF metadata handoff

Route: systematic-debugging
Owner: implementation
Input: Android imports many audio files but album/metadata fields remain fallback values, unlike the previous iOS metadata behavior.
Root cause: Android SAF scanner stored and passed `AudioSource.Uri(content://...)` into metadata enrichment. `AudioMetadataReader` intentionally returns null for `AudioSource.Uri`, and native TagLib reads filesystem paths only. The Android TagLib `.so` packaging was present, but the scan path never handed TagLib a readable file path for SAF documents.
Output: Added a separate `metadataAudioSource` to `AudioScanCandidate` so playback can preserve the original content URI while metadata enrichment can use a temporary filesystem path. Android SAF scanning now copies supported audio documents into an app cache file under `context.cacheDir/rhythhaus-taglib/<sourceId>/...` and supplies that cache path to TagLib. Common scanner regression coverage verifies metadata is read from the separate filesystem source while the stored/playback source remains the original URI.
Verification: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest.scannerCanReadMetadataFromSeparateFilesystemSourceWhilePreservingPlaybackUri' --configuration-cache` -> BUILD SUCCESSFUL; `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.LibraryScannerTest' --configuration-cache` -> BUILD SUCCESSFUL; `./gradlew :shared:compileAndroidMain :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; APK contains `librhythhaus_taglib.so` for arm64-v8a, armeabi-v7a, and x86_64. Full `:shared:jvmTest` currently has unrelated macOS native playback failures (`No native macOS player has been loaded`) in `JvmPlaybackEngineTest`, outside this Android metadata path.
Next owner: user for manual Android re-scan/runtime validation with real tagged SAF audio; clear/re-scan may be needed for already-imported fallback metadata rows.
Blockers: none for Android compile/APK packaging; full JVM suite has unrelated macOS playback test failures.

## Handoff - 2026-06-30 music progress scrubber

Route: openspec+superpowers
Owner: implementation
Input: User requested a music-player progress slider that supports single-click destination seeking and avoids multiple intermediate seeks while dragging.
Output: Added shared Compose `MusicProgressScrubber` with pure seek math and interaction-state tests; replaced expanded now-playing Miuix `Slider` with one-shot tap and drag-release seeking. Follow-up fix made scrub preview state Compose-observable and cancellation cleanup robust. `NowPlayingBar` remains passive progress.
Verification: Task 1 and Task 2 implementers ran focused `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.MusicProgressScrubberTest' --configuration-cache` and `./gradlew :shared:compileKotlinJvm --configuration-cache` with BUILD SUCCESSFUL. Task reviews passed clean after the observable-preview fix. Final `./init.sh` -> BUILD SUCCESSFUL, including shared JVM tests, desktop compile, Android debug build, Xcode 26.6 Build 17F113, and iOS simulator tests.
Next owner: user for manual Android/iOS/macOS interaction validation with real playback: tap-to-seek and drag-release seeking should produce no audible intermediate jumps.
Blockers: none for automated verification.

## Handoff - 2026-06-30 iOS audio-session UI unresponsiveness warning

Route: systematic-debugging
Owner: implementation
Input: Xcode/runtime warnings in `PlaybackEngine.ios.kt` at prepare/configure audio-session sites: `AVAudioPlayer.prepareToPlay`, `AVAudioSession.setCategory`, and `AVAudioSession.setActive` can lead to UI unresponsiveness when called on the main thread.
Root cause: shared `PlaybackController` engine work was already asynchronous, but the iOS `playbackEngineDispatcher` actual still used `Dispatchers.Main`, so iOS backend load/configuration work still ran the blocking Apple audio calls on the UI thread.
Output: Added iOS regression coverage in `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt` asserting iOS playback engine work does not use `Dispatchers.Main`; changed `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackDispatchers.ios.kt` to `Dispatchers.Default` while Android stays Main and JVM stays IO.
Verification: Targeted iOS dispatcher regression first failed, then passed. `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL. `./init.sh` -> BUILD SUCCESSFUL, including shared JVM tests, desktop compile, Android debug build, Xcode 26.6 Build 17F113, and iOS simulator tests.
Next owner: user for manual iOS runtime validation that warnings are gone during real playback load/start.
Blockers: none for automated verification.



## Handoff - 2026-06-30 iOS track-switch blast beep mitigation

Route: systematic-debugging
Owner: implementation
Input: Possible blast/beep artifact when switching between tracks.
Root cause hypothesis: iOS track switching called `release()` from `load()`, which immediately stopped the current `AVAudioPlayer` while the next player was being prepared. Abrupt stop/disposal at a non-zero waveform crossing can produce an audible transient, especially with fast auto-advance/skip.
Output: Added a dedicated iOS track-switch teardown path that fades the current `AVAudioPlayer` volume to silence over 50 ms before stopping it, without clearing Now Playing state like a full user-facing release. Added iOS regression coverage for the soft teardown constants/strategy.
Verification: Targeted iOS regression first failed to compile before the production symbols existed, then passed after implementation. `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL. `./init.sh` -> BUILD SUCCESSFUL, including shared JVM tests, desktop compile, Android debug build, Xcode 26.6 Build 17F113, and iOS simulator tests.
Next owner: user for manual iOS runtime listening test while rapidly skipping and auto-advancing between real tracks.
Blockers: none for automated verification.

## Subagent-driven execution outcome

Used Subagent-Driven Development on docs/superpowers/plans/2026-06-11-taglib-metadata-module.md (native TagLib wrapper plan). Assessment subagent confirmed: Tasks 1-3 (module/API, C ABI shim, JVM/macOS JNI) are complete with pinned upstream TagLib v2.3 FetchContent builds and real fixture tests passing; Tasks 4/5 Android/iOS remaining as honest unsupported scaffolds; Task 6 shared integration complete; Task 7 OpenSpec/docs complete.

Android native packaging subagent was dispatched but hit an HTTP 429 rate limit mid-task. Incomplete changes were cleaned up. The native TagLib Android NDK/CMake per-ABI builds remain the next feasible implementation gap.

## Completed

- Initialized a first shared Compose Multiplatform product surface for RhythHaus.
- Added shared demo music models and formatting tests.
- Scoped desktop native packaging to macOS DMG only for current target scope.
- Confirmed OpenSpec is initialized via `openspec/` and `openspec/config.yaml`.
- Created project agent harness files:
  - `AGENTS.md`
  - `docs/harness-engineering.md`
  - `init.sh`
  - `progress.md`

## In progress

- OpenSpec change `play-music-all-platforms` has first implementation slice completed and validated.
  - Proposal: `openspec/changes/play-music-all-platforms/proposal.md`
  - Design: `openspec/changes/play-music-all-platforms/design.md`
  - Spec: `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`
  - Tasks: `openspec/changes/play-music-all-platforms/tasks.md`
  - Implementation: shared playback model/controller/UI plus Android Media3, iOS AVFAudio, and macOS AVFoundation Objective-C++/JNI engine.
  - Validation: `./init.sh` -> BUILD SUCCESSFUL on 2026-06-10.
  - Follow-up backend implementation: Android playback migrated from platform `MediaPlayer` to Media3/ExoPlayer; macOS/JVM playback migrated from Java Sound `Clip` to native AVFoundation through a temporary JNA bridge, then replaced with a small Objective-C++ helper called through JNI; iOS remains on native AVFAudio `AVAudioPlayer`. MacOS/JVM playback now starts a daemon scheduled progress publisher while playing so the shared Compose progress slider advances continuously instead of only updating on play/pause/seek events.
  - Follow-up validation: added JVM regression test `nativeMacPlaybackEnginePublishesProgressWhilePlaying`; first run failed at `JvmPlaybackEngineTest.kt:105` because no periodic progress events were emitted; after the fix, targeted test passed. `openspec validate play-music-all-platforms` -> valid; `openspec validate import-local-audio` -> valid; `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; `/usr/bin/xcrun xcodebuild -version` -> Xcode 26.5 Build 17F42; `./gradlew :shared:iosSimulatorArm64Test :desktopApp:packageDmg --configuration-cache` -> BUILD SUCCESSFUL and produced `desktopApp/build/compose/binaries/main/dmg/RhythHaus-1.0.0.dmg`; `jar tf shared/build/libs/shared-jvm.jar | grep -E 'native/.*/librhythhaus_audio.dylib'` -> `native/macos-aarch64/librhythhaus_audio.dylib`.
- OpenSpec change `import-local-audio` has first manual import slice completed and validated.
  - Proposal: `openspec/changes/import-local-audio/proposal.md`
  - Design: `openspec/changes/import-local-audio/design.md`
  - Spec: `openspec/changes/import-local-audio/specs/local-audio-import/spec.md`
  - Tasks: `openspec/changes/import-local-audio/tasks.md`
  - Implementation: shared import model/UI, Android document picker, macOS/JVM native Finder-style file dialog, iOS unsupported-state placeholder.
  - Validation: `./init.sh` -> BUILD SUCCESSFUL on 2026-06-10; `openspec validate import-local-audio` -> valid.
  - Follow-up update: removed sample/demo library playback path and `AudioSource.DemoTone`; empty library now prompts for local import only. Replaced macOS/JVM Swing `JFileChooser` with native AWT `FileDialog` so macOS opens the system Finder-style panel.
  - Follow-up validation: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL; `/usr/bin/xcrun xcodebuild -version` -> Xcode 26.5 Build 17F42; `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> BUILD SUCCESSFUL; `openspec validate import-local-audio` -> valid.

## Next steps

1. Manually validate foreground play/pause/seek on Android device/emulator and macOS using real local audio files imported through the UI.
2. Verify packaged macOS DMG runtime behavior for the native AVFoundation Objective-C++/JNI helper.
3. Keep iOS playback on native Apple audio APIs; decide whether the existing Kotlin/Native `AVAudioPlayer`, AVFoundation `AVPlayer`, or a Swift bridge best fits the iOS import/media-library path.
4. Plan the iOS document-picker bridge so iOS can import local files instead of showing the current unsupported-state message.

## Handoff - 2026-06-24 Now Playing Panel + Track Ordering + Artwork Display

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Transform NowPlayingCard into clickable floating NowPlayingBar with expand-to-full-screen NowPlayingScreen; order album/artist tracks by track number instead of alphabetically; display album/track/artist artwork images everywhere instead of text placeholders.

Implementation:
- Task 1 (data models): Added trackNumber, discNumber, artworkBytes, artworkMimeType to AudioMetadata, LibraryTrack, and Track models. Updated SQLDelight schema (4 new columns in library_track table), all repository queries, library scanner, and UI-mapping functions (librarySnapshot, toUiTrack) to flow the new fields end-to-end.
- Task 2 (ordering): Changed all 4 track-grouping functions in LibraryBrowser.kt from alphabetical sortedBy to discNumber → trackNumber → title ordering.
- Task 3 (artwork display): Updated AlbumMark, AlbumCard, and ArtistRow composables to decode and show artwork Image with ContentScale.Crop when available, falling back to existing text placeholders.
- Task 4 (NowPlayingBar): Created new floating bar composable with mini progress bar, artwork thumbnail, track info, and play/pause button. Extracted HausColors.kt to share color constants.
- Task 5 (NowPlayingScreen): Created full-screen expanded view with large artwork, track metadata (including track number), seek bar, and transport controls (stop/play-pause/next).
- Task 6 (wiring): Replaced inline NowPlayingCard in LibraryHomeScreen and DrillDownView with Box-overlayed NowPlayingBar at bottom and expandable NowPlayingScreen.

Verification:
- `./init.sh`: BUILD SUCCESSFUL — shared JVM tests, desktop compile, Android debug APK, iOS simulator tests all pass.
- 7 commits: 5f93931, 8d6501a (fix), a7468bc, 17b0ebb, dbf2932, f8bf98e, 509a5a9

Acceptance:
- Requirement matched: yes for all 3 features (floating bar + expand, track-number ordering, artwork display).
- Scope controlled: yes; only data model, ordering, artwork, and UI changes. No platform-specific or unrelated changes.
- Remaining risk: (a) iOS artwork decode returns null — artwork falls back to text placeholders on iOS; (b) SQL schema migration requires fresh install for existing dev databases; (c) manual visual confirmation of artwork rendering with real embedded-artwork audio files on Android/macOS.

Changed files:
- AudioMetadata.kt, MusicModels.kt, LibraryModels.kt: extended data models
- LibraryTrack.sq: SQL schema +4 columns
- SqlDelightLibraryRepository.kt: new column read/write
- LibraryScanner.kt: pass metadata through
- LibraryBrowser.kt: track-number ordering
- App.kt: artwork display in 3 composables + wiring NowPlayingBar/Screen
- HausColors.kt, NowPlayingBar.kt, NowPlayingScreen.kt: new composables

Next owner: user for manual visual/runtime validation.
Blockers: none for compile/test verification.

## Decisions

- First platform scope: Android, iOS, macOS/desktop JVM.
- Windows/Linux support is future scope only.
- Use shared-first Compose Multiplatform UI.
- OpenSpec owns durable requirements/specs/tasks because `openspec/` exists.
- Playback backend direction: Android uses Media3/ExoPlayer, iOS uses native Apple audio APIs, and macOS uses a native AVFoundation Objective-C++/JNI helper rather than Java Sound or JNA for product-grade playback.
- Superpowers owns clarification, brainstorming, task execution discipline, and TDD-style implementation loops for durable work.
- Do not create `feature_list.json` for OpenSpec-owned tasks.
- Completed OpenSpec + Superpowers workflow changes should be committed by default unless the user explicitly says not to commit.
- Commit messages should use semantic/conventional style such as `feat: ...`, `fix: ...`, `docs: ...`, `test: ...`, or `chore: ...`.
- Harness owns verification, acceptance, scope, lifecycle, and handoff evidence.

## Verification evidence

Latest successful harness verification:

```bash
./init.sh
```

Result: BUILD SUCCESSFUL for both Gradle phases. Details from 2026-06-10 playback implementation:

- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.5, Build version 17F42.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.

Harness verification command to use going forward:

```bash
./init.sh
```

## Changed files in current playback work

- `gradle/libs.versions.toml` - added shared coroutine dependency alias.
- `shared/build.gradle.kts` - added `kotlinx-coroutines-core` to common code, Android Activity Compose and Media3 to Android shared source set, and a macOS native audio helper build/resource task for JVM.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt` - shared playback domain, controller, engine contract, fake engine, and formatting helper.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/AudioImport.kt` - shared imported-audio model, import launcher contract, and imported-library mapping.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/MusicModels.kt` - added `AudioSource` to `Track` so imported rows are playable.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` - shared now-playing playback controls, import card, seek display, status/error display, and accessibility content descriptions.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt` - Android Media3/ExoPlayer engine with context-backed URI playback.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/AudioImport.android.kt` - Android `OpenMultipleDocuments` audio picker.
- `androidApp/src/main/kotlin/com/eterocell/rhythhaus/MainActivity.kt` - provides Android application context for content URI playback.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt` - iOS `AVAudioPlayer` engine and foreground audio session setup.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/AudioImport.ios.kt` - iOS unsupported import placeholder with user-facing copy.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt` - macOS native AVFoundation-backed playback engine through an Objective-C++/JNI helper.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/AudioImport.jvm.kt` - macOS/JVM native AWT file dialog importer.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/SharedCommonTest.kt` - playback and import mapping tests.
- `openspec/changes/play-music-all-platforms/design.md` - recorded first-slice engine and format decisions.
- `openspec/changes/play-music-all-platforms/tasks.md` - marked implemented/verified tasks and remaining manual validation.
- `openspec/changes/import-local-audio/*` - planned, specified, and task-tracked manual local audio import.
- `progress.md` - updated handoff/evidence.

## Completion evidence checklist

- [x] Workflow route recorded: `openspec+superpowers`.
- [x] Current owner recorded: harness-creator for harness files; OpenSpec for future durable product tasks.
- [x] Fact source conflict avoided: no `feature_list.json` created because OpenSpec is initialized.
- [x] Verification commands documented in `AGENTS.md`, `docs/harness-engineering.md`, and `init.sh`.
- [x] Known platform scope recorded.
- [x] Next safe action recorded.

## Handoff

Route: openspec+superpowers
Owner: implementation
Input: corrected Task 7 request to record native TagLib wrapper architecture and current platform state in OpenSpec/progress/docs
Output: `openspec/changes/import-local-audio/design.md` documents that rich import metadata flows through the native `:taglib` wrapper seam, not hand-written Kotlin parsers; `openspec/changes/import-local-audio/tasks.md` tracks the documentation follow-up and remaining native TagLib linking/packaging work; `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md` is preserved as the corrected native-wrapper plan and points the next action at real TagLib library linking per platform.
Next owner: implementation for platform native TagLib linking/packaging
Blockers: real rich metadata support remains blocked until native TagLib is linked/packaged per platform: macOS/JVM helper currently supports skeleton unsupported behavior unless TagLib is available at build/link time; Android has JNI-shaped scaffold but no packaged native library; iOS has unsupported scaffold and documented expected native layout but no cinterop yet.

## Handoff - 2026-06-11 native TagLib metadata docs

Route: openspec+superpowers
Owner: implementation
Scope: OpenSpec/progress/docs only; no source/build files changed.
Verification:
- `openspec validate import-local-audio --strict`: pass (`Change 'import-local-audio' is valid`).
Acceptance:
- Requirement matched: yes; docs record native TagLib wrapper architecture and reject hand-written Kotlin metadata parsing.
- Scope controlled: yes; changes limited to import-local-audio OpenSpec docs, progress, and the corrected Superpowers plan.
- Edge cases/risk reviewed: Android/iOS/macOS current support is documented honestly; no completed Android/iOS rich metadata support is claimed.
Changed files:
- `openspec/changes/import-local-audio/design.md`: native-wrapper metadata architecture, current platform state, and linking/packaging risks.
- `openspec/changes/import-local-audio/tasks.md`: Task 5 documentation follow-up and remaining real TagLib linking task.
- `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md`: corrected plan included under docs and updated Task 7/current next action.
- `progress.md`: handoff evidence for this docs task.
Next owner: implementation
Blockers: none for docs; real metadata support still requires linking/packaging native TagLib libraries per platform before claiming full support.
Commit: docs task commit created after this handoff update with message `docs: record native taglib import metadata plan`.

## Handoff - 2026-06-11 native TagLib wrapper full verification

Route: openspec+superpowers
Owner: harness-creator
Scope: verification-only final review for native `:taglib` wrapper work at HEAD `e54d788`; no source changes.
Verification:
- Initial `git status --short && git rev-parse --short HEAD`: pass; worktree was clean and HEAD was `e54d788`.
- `./init.sh`: pass; completed with `=== Harness verification complete ===` after Gradle reported `BUILD SUCCESSFUL` for the documented harness phases.
- `./gradlew :taglib:jvmTest :taglib:assembleAndroidMain :taglib:iosSimulatorArm64Test --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL in 1s`, 30 actionable tasks, configuration cache entry stored.
- `cmake -S taglib/native -B taglib/build/cmake-verify && cmake --build taglib/build/cmake-verify`: pass; CMake configured and built `librhythhaus_taglib.dylib`. Output also reported `TagLib was not found by CMake find_package(TagLib) or pkg-config; building unsupported shim skeleton only.` Shell startup emitted non-fatal local profile noise: `bash: ${candidate_name^^}: bad substitution` and missing broot launcher path.
- `openspec validate import-local-audio --strict`: pass; output `Change 'import-local-audio' is valid`.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `taglib/src`: pass; no matches.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `shared/src`: pass; only `Uri.parse(value)` in Android playback URI conversion matched, not metadata parsing.
Acceptance:
- Requirement matched: yes; full harness, focused taglib Gradle tasks, CMake shim configure/build, OpenSpec validation, and no-Kotlin-parser search were executed successfully.
- Scope controlled: yes; verification evidence only.
- Edge cases/risk reviewed: real TagLib linkage/packaging remains incomplete per platform; current CMake build confirms the unsupported skeleton path when TagLib is not discoverable locally.
Changed files:
- `progress.md`: added this final verification evidence.
Next owner: implementation or user for real TagLib linkage/packaging per platform.
Blockers: none for verification; remaining product limitation is that rich metadata support still needs real TagLib library linkage/packaging on macOS/JVM, Android, and iOS before claiming full platform metadata support.
Commit: docs verification commit with message `docs: record native taglib verification evidence`.

## Handoff - 2026-06-11 upstream TagLib JVM/macOS verification

Route: openspec+superpowers
Owner: harness-creator
Scope: verification and evidence recording for upstream TagLib JVM/macOS build/link correction at HEAD `3849c941769890862bba3da89fef3303ec679b8c`; no source/build files changed.
Upstream TagLib correction commits:
- `aa16f826` `feat: build upstream taglib for jvm reader`: Gradle fetches/builds pinned upstream `https://github.com/taglib/taglib` v2.3 commit `1b94b93762636ebe5733180c3e825be4621e4c7f`, statically links `libtag.a` into the macOS/JVM JNI helper, and builds with `RH_TAGLIB_HAS_TAGLIB=1`.
- `ae30fd1` `test: verify jvm taglib reads real fixture`: JVM test generates a WAV RIFF/INFO fixture and asserts real `createTagLibReader`/`readPath` returns `Found` through JNI/C ABI/upstream TagLib.
- `3849c94` `docs: plan upstream taglib mobile builds`: OpenSpec/docs clarify Android/iOS still need upstream TagLib builds packaged and wired from the same pinned source.
Verification:
- Initial `git status --short && git rev-parse HEAD`: pass; worktree was clean and HEAD was `3849c941769890862bba3da89fef3303ec679b8c`.
- `./init.sh`: pass; completed with `=== Harness verification complete ===` after Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :taglib:allTests --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; `:taglib:checkoutUpstreamTagLib` output `HEAD is now at 1b94b93 Version 2.3`; `:taglib:jvmTest` and `:taglib:iosSimulatorArm64Test` passed/up-to-date.
- `./gradlew :taglib:buildMacosTagLibHelper --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; shell startup emitted non-fatal local profile noise `bash: ${candidate_name^^}: bad substitution` and missing broot launcher path.
- `openspec validate import-local-audio --strict`: pass; output `Change 'import-local-audio' is valid`.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `taglib/src`: pass; no matches.
- Targeted parser search `ID3|FLAC|MP4|parse|parser|TagFormat` under `shared/src`: pass; only Android playback URI conversion matched (`AudioSource.Uri -> Uri.parse(value)`), unrelated to metadata parsing.
- Linkage check on `taglib/build/generated/nativeTagLibResources/jvmMain/native/macos-aarch64/librhythhaus_taglib.dylib`: pass; `file` reported `Mach-O 64-bit dynamically linked shared library arm64`; `otool -L` showed only system dylibs (`libc++.1.dylib`, `libSystem.B.dylib`) besides itself, consistent with static TagLib linkage; `nm -gU ... | grep -E 'TagLib|rh_taglib|Java_com_eterocell_rhythhaus_taglib'` showed `_Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative` and many `TagLib` symbols such as `__ZN6TagLib10ByteVector10fromBase64ERKS0_`.
Acceptance:
- Requirement matched: yes; JVM/macOS now actually builds, statically links, and tests upstream TagLib v2.3 from the pinned upstream commit.
- Scope controlled: yes; only `progress.md` evidence changed in this task.
- No custom Kotlin parser claim: confirmed; targeted source search did not find metadata parser implementation under `taglib/src` or `shared/src`.
Remaining limitations:
- Android still needs upstream TagLib v2.3 built for supported ABIs, packaged, and wired through JNI before Android rich metadata support can be claimed.
- iOS still needs upstream TagLib v2.3 built/packaged, cinterop/native wiring completed, and tests before iOS rich metadata support can be claimed.
- The project must not claim a custom Kotlin metadata parser; metadata support is through native TagLib wrapper/linkage.
Changed files:
- `progress.md`: added this upstream TagLib JVM/macOS verification handoff/evidence.
Next owner: implementation for Android/iOS upstream TagLib packaging and wiring.
Blockers: none for JVM/macOS verification; remaining product limitation is mobile native TagLib packaging/wiring.
Commit: docs verification commit with message `docs: record upstream taglib verification evidence`.

## Handoff - 2026-06-11 CMake FetchContent TagLib final verification

Route: openspec+superpowers
Owner: harness-creator
Scope: final verification and evidence recording for CMake FetchContent TagLib refactor at HEAD `f263e987db85e4dc70e9e69a00203e3d1f858426`; no source/build files changed.
CMake FetchContent correction:
- Upstream TagLib import/build now lives self-contained in `taglib/native/CMakeLists.txt` via CMake `FetchContent` pinned to `1b94b93762636ebe5733180c3e825be4621e4c7f`.
- Gradle no longer performs upstream git clone/checkout; it invokes CMake and copies the generated helper dylib into JVM resources.
Verification:
- Initial `git status --short && git rev-parse HEAD`: pass; worktree was clean and HEAD was `f263e987db85e4dc70e9e69a00203e3d1f858426`.
- `./gradlew :taglib:buildMacosTagLibHelper --rerun-tasks --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; CMake configured/generated and built `librhythhaus_taglib.dylib`; output noted bundled utfcpp from TagLib source and non-fatal local shell startup noise from the user's bash profile.
- `./gradlew :taglib:jvmTest --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; `:taglib:jvmTest` was up-to-date with helper built/up-to-date.
- `./gradlew :taglib:allTests --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`; JVM and iOS simulator taglib tests were up-to-date.
- `openspec validate import-local-audio --strict`: pass; output `Change 'import-local-audio' is valid`.
- Linkage check on `taglib/build/native/macosTagLibHelper-arm64/librhythhaus_taglib.dylib`: pass; `otool -L` showed only itself plus system `libc++.1.dylib` and `libSystem.B.dylib`; `nm -gU` showed exported JNI symbol `_Java_com_eterocell_rhythhaus_taglib_NativeTagLibBridge_readPathNative`.
- Targeted Gradle search in `taglib/build.gradle.kts` for `git clone|checkoutUpstreamTagLib|Exec\(|git\s+checkout`: pass; no matches.
- Targeted Kotlin parser search in `taglib/src` for parser signatures/ID3/MPEG/RandomAccessFile/ByteBuffer/synchsafe/readBytes: pass; no matches.
Acceptance:
- Requirement matched: yes; CMake-owned upstream TagLib import was freshly verified with build/test/OpenSpec/linkage/search evidence.
- Scope controlled: yes; only `progress.md` evidence changed in this task.
- No custom Kotlin parser claim: confirmed by targeted search; metadata remains through the native TagLib wrapper path.
Changed files:
- `progress.md`: added this CMake FetchContent final verification handoff/evidence.
Next owner: implementation/user for any remaining Android/iOS native TagLib packaging/wiring beyond this macOS/JVM verification.
Blockers: none for this verification.
Commit: docs verification commit with message `docs: record cmake taglib import evidence`.

## Handoff - 2026-06-11 local folder scanning SQLDelight setup

Route: openspec+superpowers
Owner: implementation
Scope: Task 2 only for `scan-local-audio-folders`: SQLDelight version catalog aliases, shared module SQLDelight plugin/database configuration, platform driver dependencies, and initial library schema/queries.
Verification:
- Initial `git status --short --branch`: pass; worktree was clean on `main...egl/main` before edits.
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: initial fail in `:shared:generateCommonMainRhythHausDatabaseInterface` because default SQLDelight SQLite 3.18 dialect did not parse `INSERT ... ON CONFLICT ... DO UPDATE` from the approved plan schema.
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: pass after configuring SQLDelight SQLite 3.38 dialect; Gradle reported `BUILD SUCCESSFUL in 5s`, with `:shared:generateCommonMainRhythHausDatabaseInterface` up-to-date and `:shared:compileKotlinMetadata SKIPPED`.
- `openspec validate scan-local-audio-folders --strict`: pass; output `Change 'scan-local-audio-folders' is valid`.
Acceptance:
- Requirement matched: yes; SQLDelight 2.3.2 aliases/plugin/dependencies and `RhythHausDatabase` schema were added, with `resources.srcDir(nativeAudioResourceRoot)` preserved.
- Scope controlled: yes; no feature code beyond database build setup/schema.
- Edge cases/risk reviewed: explicit SQLite 3.38 dialect is required for planned upsert syntax.
Changed files:
- `gradle/libs.versions.toml`: SQLDelight version, runtime/coroutines/platform driver libraries, plugin alias.
- `shared/build.gradle.kts`: SQLDelight plugin/database configuration, platform dependencies, preserved JVM native resource source dir.
- `shared/src/commonMain/sqldelight/com/eterocell/rhythhaus/library/RhythHausDatabase.sq`: initial library source/track/scan schema and queries.
- `openspec/changes/scan-local-audio-folders/tasks.md`: marked dependency setup and focused verification complete.
- `progress.md`: recorded this handoff evidence.
Next owner: implementation for Task 3 shared library domain models.
Blockers: none.
Commit: semantic commit with message `build: add library database setup`.


## Handoff - 2026-06-23 subagent-driven scanner/source access slice

Route: openspec+superpowers
Owner: implementation
Scope: subagent-driven implementation slice for `scan-local-audio-folders` tasks 1.1, 3.1-3.5, and 4.1-4.3; no UI changes and no repository schema changes.
Subagent inputs:
- Scanner/source-access review found missing iOS actual, missing Android DocumentFile dependency, automatic remove-missing data-loss risk, and metadata-reader failure risk.
- Gradle/database review confirmed SQLDelight setup tasks 1.2/1.3 were already complete and recommended `./gradlew :shared:compileKotlinMetadata --configuration-cache`.
- Slice planning recommended scanner orchestration and platform source access as conflict-safe next tasks, reserving OpenSpec/progress updates for the coordinator.
Implementation:
- Added Android `androidx.documentfile:documentfile` dependency for SAF tree traversal.
- Added iOS app-local folder picker/source scanner actual for `rememberPlatformFolderPickerLauncher` and `IOSAppLocalSourceAccess`.
- Kept Android SAF and JVM folder source access seams compile-safe across targets.
- Changed `LibraryScanner` to preserve already imported tracks after a completed scan instead of automatically deleting missing tracks; explicit remove-missing remains a later UI/action task.
- Changed metadata enrichment to fall back to filename metadata if `AudioMetadataReader.read` throws.
- Added common scanner tests for non-destructive completed scans and metadata-reader failure fallback.
Verification:
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: pass after source-access seam fixes; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :shared:compileKotlinMetadata :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `/usr/bin/xcrun xcodebuild -version`: pass; Xcode 26.5 Build 17F42.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: initial failures in the new iOS source-access actual, then pass after correcting enum/API usage; final Gradle run reported `BUILD SUCCESSFUL`.
- `openspec validate scan-local-audio-folders --strict`: pass; output `Change 'scan-local-audio-folders' is valid`.
Acceptance:
- Requirement matched: yes for scanner contracts/orchestration/cancellation/metadata fallback and Android/JVM/iOS first source access implementations.
- Scope controlled: yes; shared library manager UI, platform-focused source tests, full `./init.sh`, and archive remain open.
- Edge cases/risk reviewed: unsupported-file accounting may still need product/UI tuning; iOS app-local source uses a deterministic `createdAtEpochMillis = 0L` until a shared clock/source factory is introduced; explicit remove-missing action is still pending.
Changed files:
- `gradle/libs.versions.toml`: Android DocumentFile alias.
- `shared/build.gradle.kts`: Android DocumentFile dependency.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/LibraryScanner.kt`: metadata fallback and non-destructive scan completion.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.kt`: source picker/access contract and shared source-local key helpers.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`: Android SAF picker/source access.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.jvm.kt`: macOS/JVM native folder picker/source access.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.ios.kt`: iOS app-local folder source access.
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/library/LibraryScannerTest.kt`: scanner regression coverage.
- `openspec/changes/scan-local-audio-folders/tasks.md`: marked completed tasks for this slice only.
- `progress.md`: recorded this handoff evidence.
Next owner: implementation for platform-focused tests where practical and shared library manager UI tasks 5.1-5.4.
Blockers: none for this slice; full completion still requires UI integration, platform-focused tests, full `./init.sh`, and final OpenSpec archival.

## Handoff - 2026-06-23 Android Media3 system controls slice

Route: openspec+superpowers
Owner: implementation
Scope: user-requested Android platform audio API/control-panel slice for `play-music-all-platforms`; no iOS/macOS media-control changes and no long-running background playback claim.
Implementation:
- Added Media3 Session dependency for Android shared playback.
- Wrapped the active Android ExoPlayer in a Media3 `MediaSession` and released it with the player.
- Built Android Media3 `MediaItem`/`MediaMetadata` from shared `PlayableTrack` title, artist, album, id, and source so Android system media controls can show current track information and transport controls.
- Added Android host regression coverage for the metadata exposed to platform controls.
- Updated OpenSpec design/spec/tasks for the Android system media-controls scope while keeping background playback out of scope.
Verification:
- `./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' --configuration-cache`: initial RED failed on missing helper, then pass after implementation; final Gradle run reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
- `./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
Acceptance:
- Requirement matched: yes for Android platform media session/metadata wiring to support system control-panel display and transport controls during playback.
- Scope controlled: yes; no foreground service, notification-service manifest, iOS Now Playing, macOS remote controls, or background playback support was added.
- Edge cases/risk reviewed: emulator/device manual validation is still required to visually confirm Android control-panel rendering with a real playable local file.
Changed files:
- `gradle/libs.versions.toml`: Media3 Session alias.
- `shared/build.gradle.kts`: Android Media3 Session dependency.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`: MediaSession lifecycle and track metadata MediaItem construction.
- `shared/src/androidHostTest/kotlin/com/eterocell/rhythhaus/AndroidPlaybackMediaSessionTest.kt`: Android metadata regression test.
- `openspec/changes/play-music-all-platforms/design.md`: Android system media-controls scope and non-goal adjustment.
- `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`: Android system controls scenario.
- `openspec/changes/play-music-all-platforms/tasks.md`: completed Android media-session task and remaining manual device check.
- `progress.md`: recorded this handoff evidence.
Next owner: implementation/user for Android emulator/device manual validation with real playback and system control-panel observation.
Blockers: none for compile/test verification; visual Android control-panel confirmation requires an emulator/device playback session.

## Handoff - 2026-06-23 all-platform system media controls correction

Route: openspec+superpowers
Owner: implementation
Scope: correction to extend the platform media-control slice beyond Android to all first platforms: Android, iOS, and macOS. No long-running background playback service/notification support was added.
Implementation:
- iOS `AVAudioPlayer` engine now updates and clears `MPNowPlayingInfoCenter` with title, artist, album, elapsed time, and duration on load/play/pause/stop/seek/release.
- macOS/JVM AVFoundation helper now links `MediaPlayer.framework`, exposes JNI methods for Now Playing metadata/position/clear operations, and updates `MPNowPlayingInfoCenter` through the Objective-C++ helper.
- Android Media3 session/metadata wiring from the prior slice remains in place.
- Added focused iOS and macOS/JVM tests for the new metadata seams.
- Updated OpenSpec design/spec/tasks to describe platform system media controls across Android, iOS, and macOS rather than Android only.
Verification:
- `./gradlew :shared:iosSimulatorArm64Test --tests 'com.eterocell.rhythhaus.IOSNowPlayingInfoTest' --configuration-cache`: initial RED failed on missing `buildIOSNowPlayingInfo`, then pass after implementation; final Gradle run reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.macOSNowPlayingInfoUpdateAcceptsTrackMetadata' --configuration-cache`: initial RED failed on missing bridge methods, then failed once on missing `MediaPlayer.framework` link, then pass after linking; final Gradle run reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
- `./gradlew :shared:jvmTest :shared:iosSimulatorArm64Test :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.AndroidPlaybackMediaSessionTest' :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
Acceptance:
- Requirement matched: yes for platform-native media information/control seams on Android, iOS, and macOS foreground playback sessions.
- Scope controlled: yes; no streaming, background playback guarantee, Android foreground-service notification, iOS background mode, or macOS menu-bar/remote-control UI was added.
- Edge cases/risk reviewed: real system media-control rendering still requires manual validation on Android device/emulator, iOS simulator/device Control Center, and macOS Now Playing/Control Center with a real local audio file.
Changed files:
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: iOS Now Playing metadata lifecycle.
- `shared/src/iosTest/kotlin/com/eterocell/rhythhaus/IOSNowPlayingInfoTest.kt`: iOS metadata regression test.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: macOS bridge calls for Now Playing metadata/position.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: macOS `MPNowPlayingInfoCenter` native helper implementation.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`: macOS bridge metadata regression test.
- `shared/build.gradle.kts`: links macOS helper with `MediaPlayer.framework` and keeps Android Media3 Session dependency.
- `openspec/changes/play-music-all-platforms/design.md`: all-platform media-controls design update.
- `openspec/changes/play-music-all-platforms/specs/audio-playback/spec.md`: iOS/macOS system media-controls scenarios.
- `openspec/changes/play-music-all-platforms/tasks.md`: iOS/macOS media-controls tasks and manual validation follow-ups.
- `progress.md`: recorded this correction handoff evidence.
Next owner: implementation/user for manual platform validation with real playback on Android, iOS, and macOS.
Blockers: none for compile/test verification; visual system media-control confirmation requires platform runtime sessions.

## Handoff - 2026-06-23 macOS Now Playing visibility fix

Route: openspec+superpowers
Owner: implementation
Scope: macOS-specific fix after manual runtime feedback that metadata did not appear in macOS Control Center while music was playing.
Root cause:
- The native helper populated `MPNowPlayingInfoCenter.nowPlayingInfo`, but did not set `MPNowPlayingInfoCenter.playbackState` or `MPNowPlayingInfoPropertyPlaybackRate` during play/pause/stop transitions. macOS Control Center can treat metadata-only updates as inactive, so the session may not surface while playing.
Implementation:
- Added macOS bridge playback-state update API and regression coverage.
- `MacOSNativePlaybackEngine` now updates native Now Playing playback state on play, pause, and stop.
- Objective-C++ helper now sets `MPNowPlayingInfoCenter.playbackState` and `MPNowPlayingInfoPropertyPlaybackRate`, and clears state on release.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.macOSNowPlayingPlaybackStateUpdatesForControlCenterVisibility' --configuration-cache`: initial RED failed on missing bridge method, then pass after implementation; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
Acceptance:
- Requirement matched: yes for the identified missing macOS active playback-state signal needed by system media controls.
- Scope controlled: yes; no unrelated platform changes or background playback service support added in this fix.
- Remaining risk: visual confirmation still requires running desktop playback with a real audio file and checking macOS Control Center/Now Playing UI.
Changed files:
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: playback-state bridge calls and status mapping.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: native playback state/rate updates.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`: macOS playback-state regression test.
- `progress.md`: recorded this bugfix evidence.
Next owner: user/implementation for manual macOS Control Center confirmation.
Blockers: none for compile/test verification.

## Handoff - 2026-06-23 macOS remote command registration fix

Route: openspec+superpowers
Owner: implementation
Scope: second macOS-specific runtime fix after Control Center still did not show media information with metadata plus playback state/rate.
Root cause hypothesis:
- Metadata fields were not the likely weak point: title, artist, album, duration, elapsed time, playback rate, and playback state were already populated. The missing native seam was `MPRemoteCommandCenter` registration, so macOS may not classify the JVM process as a controllable Now Playing media session.
Implementation:
- Added `MacAudioPlayerBridge.registerNowPlayingRemoteCommands()` and calls it when a macOS track is loaded.
- Native helper now enables play, pause, toggle play/pause, stop, and change playback position commands via `MPRemoteCommandCenter` and maps them to the active `AVAudioPlayer`.
- Added focused JVM regression coverage for remote command registration.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.JvmPlaybackEngineTest.macOSNowPlayingRegistersRemoteCommandsForControlCenter' --configuration-cache`: initial RED failed on missing bridge method, then pass after implementation; Gradle reported `BUILD SUCCESSFUL`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin --configuration-cache`: pass; Gradle reported `BUILD SUCCESSFUL`.
- `openspec validate play-music-all-platforms --strict`: pass; output `Change 'play-music-all-platforms' is valid`.
Acceptance:
- Requirement matched: yes for the missing native remote-command/control registration needed for macOS Control Center discoverability.
- Scope controlled: yes; fix is limited to the macOS native helper/JVM bridge and tests.
- Remaining risk: visual confirmation still requires running desktop playback and checking macOS Control Center/Now Playing UI.
Changed files:
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: registers remote commands when loading a track.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: registers `MPRemoteCommandCenter` handlers.
- `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/JvmPlaybackEngineTest.kt`: remote-command registration regression test.
- `progress.md`: recorded this runtime fix evidence.
Next owner: user/implementation for manual macOS Control Center confirmation.
Blockers: none for compile/test verification.

## Handoff - 2026-06-23 Android TagLib native packaging completion

Route: openspec+superpowers
Owner: implementation
Scope: Complete `feat/taglib-metadata-module` Android native TagLib packaging from the preserved Superpowers plan `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md` and OpenSpec follow-up `openspec/changes/import-local-audio/tasks.md`.
Implementation:
- `taglib/build.gradle.kts` now builds pinned upstream `github.com/taglib/taglib` v2.3 commit `1b94b93762636ebe5733180c3e825be4621e4c7f` with Android NDK/CMake for `arm64-v8a`, `armeabi-v7a`, and `x86_64`.
- Generated `librhythhaus_taglib.so` slices are copied into `taglib/src/androidMain/jniLibs/<abi>/` by `packageAndroidTagLibJniLibs`; `.gitignore` keeps generated binaries out of source control.
- Android packaging hooks make TagLib Android JNI/native merge tasks depend on the native build/copy task.
Verification:
- `./gradlew :taglib:buildAllAndroidTagLibHelpers --configuration-cache`: pass; all three ABI helpers built.
- `./gradlew :taglib:allTests --configuration-cache`: pass.
- `./gradlew :taglib:assembleAndroidMain :androidApp:assembleDebug --configuration-cache`: pass.
- `unzip -l taglib/build/outputs/aar/taglib.aar | grep 'librhythhaus_taglib.so'`: pass; AAR contains `jni/arm64-v8a`, `jni/armeabi-v7a`, and `jni/x86_64` slices.
- `unzip -l androidApp/build/outputs/apk/debug/androidApp-debug.apk | grep 'librhythhaus_taglib.so'`: pass; APK contains `lib/arm64-v8a`, `lib/armeabi-v7a`, and `lib/x86_64` slices.
Acceptance:
- Requirement matched: Android native TagLib packaging from pinned upstream v2.3 is complete enough for AAR/APK packaging verification.
- Scope controlled: no Kotlin metadata parser added; iOS remains honestly pending.
- Remaining risk: Android content URI metadata still needs app-cache file path handoff and device/emulator runtime metadata validation before claiming end-to-end SAF rich metadata.
Next owner: implementation for Android content-URI-to-file handoff/runtime validation, or iOS TagLib XCFramework/cinterop packaging.
Blockers: none for Android native library packaging; iOS rich metadata support remains blocked on native static library/XCFramework/cinterop work.

## Handoff - 2026-06-23 iOS TagLib cinterop completion

Route: openspec+superpowers
Owner: implementation
Scope: Complete iOS TagLib cinterop from plan `docs/superpowers/plans/2026-06-11-taglib-metadata-module.md` Task 5 and OpenSpec `openspec/changes/import-local-audio/tasks.md` 5.5.
Implementation:
- `taglib/native/CMakeLists.txt` supports `RHYTHHAUS_TAGLIB_BUILD_STATIC=ON` for iOS static library builds.
- `taglib/build.gradle.kts` builds pinned upstream TagLib v2.3 for `iosArm64` (device) and `iosSimulatorArm64` (simulator) as static `librhythhaus_taglib.a`.
- `taglib/src/nativeInterop/cinterop/rh_taglib.def` declares cinterop binding; a generated `.def` in the build directory resolves the per-target absolute path to `librhythhaus_taglib.a`.
- `taglib/src/iosMain/kotlin/com/eterocell/rhythhaus/taglib/TagLibReader.ios.kt` now calls `rh_taglib_read_path`/`rh_taglib_free_result` through Kotlin/Native cinterop instead of returning unsupported.
- `taglib/gradle.properties` enables cinterop commonization.
- `taglib/src/nativeInterop/cinterop/.gitignore` keeps generated `.a` out of git.
Verification:
- `./gradlew :taglib:iosSimulatorArm64Test --configuration-cache`: pass; includes native CMake build, cinterop generation, Kotlin compilation, linking, and test execution.
Acceptance:
- Requirement matched: iOS TagLib cinterop links real upstream TagLib v2.3 through the C ABI shim.
- Scope controlled: no Kotlin metadata parser added.
- Remaining risk: runtime metadata validation with real audio files on device/simulator remains a manual follow-up.
Next owner: user/implementation for manual iOS playback/metadata runtime validation.
Blockers: none.

## Handoff - 2026-06-23 developer panel for TagLib metadata

Route: openspec+superpowers
Owner: implementation
Scope: shared Compose UI developer panel that displays native TagLib-parsed metadata on the main library page.
Implementation:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` adds a collapsible `DeveloperPanel` on the main `LibraryHomeScreen`, rendered between the import card and now-playing card.
- The panel lists each imported file with its source handle and the parsed `AudioMetadata` (title, artist, album, duration) returned by the native `:taglib` wrapper, or a clear "metadata unavailable" line when the native reader returns no tags.
- `LibraryHomeScreen` now receives `importedFiles` so the panel can show raw parsed results without altering the existing library/track models.
Verification:
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: BUILD SUCCESSFUL.
- `./init.sh`: BUILD SUCCESSFUL (JVM tests, desktop compile, Android debug build, iOS simulator shared tests).
Acceptance:
- Requirement matched: developer panel exists on the main page and displays TagLib-parsed metadata for imported files.
- Scope controlled: shared UI only; no model/engine/native changes.
- Remaining risk: real device/runtime metadata values still require manual import validation with real audio files.
Next owner: user/implementation for manual import + metadata runtime validation.
Blockers: none.

## Handoff - 2026-06-24 artwork in platform system media controls

Route: openspec+superpowers
Owner: implementation
Scope: Pass embedded artwork from TagLib-parsed tracks through `PlayableTrack` into Android, macOS, and iOS system media controls (notification center, Control Center, Now Playing widget).

Implementation:
- Added `artworkBytes: ByteArray?` to `PlayableTrack` with correct ByteArray equals/hashCode.
- Pass `track.artworkBytes` through `Track.toPlayableTrack()` in `App.kt`.
- Android: `buildAndroidPlaybackMediaMetadata` sets artwork data via `MediaMetadata.Builder.setArtworkData(byte[])`.
- macOS: Added `artwork` property and `setArtworkFromBytes:` method to `RhythHausAudioPlayer` native helper (ObjC++). Artwork is preserved across all now-playing info update paths. Added `nativeSetArtwork` JNI method. Gradle build links `AppKit.framework` for `NSImage`.
- JVM bridge: `MacAudioPlayerBridge.setArtwork()` calls `nativeSetArtwork` when loading a track.
- iOS: Artwork skipped (`MPMediaItemPropertyArtwork` deferred) — Kotlin/Native cinterop for `ByteArray → NSData → UIImage → MPMediaItemArtwork` requires stable Foundation bridging APIs not available in current KMP version. App's own Compose `NowPlayingCard` still displays artwork.

Verification:
- `./init.sh`: pass. BUILD SUCCESSFUL for shared JVM tests + desktop compile + Android debug build + iOS simulator tests.
- `openspec validate play-music-all-platforms --strict`: pass.
- `openspec validate import-local-audio --strict`: pass.

Acceptance:
- Requirement matched: yes for Android and macOS system media controls; iOS deferred (cinterop limitation documented).
- Scope controlled: yes; no background playback, notification, or unrelated platform changes.
- Remaining risk: visual confirmation requires running desktop/Android playback with real embedded-artwork audio files and checking system Control Center/notification artwork rendering.

Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt`: `PlayableTrack.artworkBytes` field + equals/hashCode.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: `toPlayableTrack()` passes artwork.
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.android.kt`: `buildAndroidPlaybackMediaMetadata` sets artwork data.
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: added deferred-iOS-artwork comment.
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.jvm.kt`: bridge `setArtwork` + `nativeSetArtwork` JNI declaration.
- `shared/src/nativeInterop/macos/rhythhaus_audio.mm`: artwork property, setter, JNI method, preserved across all now-playing updates.
- `shared/build.gradle.kts`: linked `AppKit.framework` for macOS `NSImage`.

Next owner: user/implementation for manual macOS/Android artwork runtime confirmation and iOS cinterop artwork follow-up.
Blockers: none for compile/test; iOS system Control Center artwork deferred.

## Handoff - 2026-06-25 UI polish + iOS lockscreen fixes

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Three independent bugfixes: button font consistency (Clear Library → Black), NowPlayingScreen next-track UI staleness (LaunchedEffect sync), iOS lockscreen player panel (MPRemoteCommandCenter + playbackState).

Implementation:
- Task 1 (26b5c47): Changed Clear Library button `fontWeight` from `FontWeight.Medium` to `FontWeight.Black` in `ImportAudioCard` to match the "Add music folder" button.
- Task 2 (6129b35): Added `LaunchedEffect(playbackState.currentTrack?.id)` in both `LibraryHomeScreen` and `DrillDownView` to sync local `selectedTrackId` with the controller's current track when advancing via next-track button or playback completion.
- Task 3 (daf1811): Registered `MPRemoteCommandCenter` handlers (play, pause, togglePlayPause, stop, changePlaybackPosition) in iOS `PlaybackEngine.ios.kt` via block-based callbacks. Set `playbackState` on `MPNowPlayingInfoCenter` on play/pause/stop/release. Added `MPNowPlayingInfoPropertyPlaybackRate` to nowPlayingInfo dictionary. Used `ULong` values for `playbackState` (0uL/1uL/2uL) and top-level `MPRemoteCommandHandlerStatusSuccess` constant.

Verification:
- `./init.sh`: BUILD SUCCESSFUL — all platforms pass (JVM tests, desktop compile, Android debug, iOS simulator tests).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: BUILD SUCCESSFUL.
- `./gradlew :shared:compileKotlinMetadata --configuration-cache`: BUILD SUCCESSFUL.

Acceptance:
- Requirement matched: yes for all 3 bugfixes.
- Scope controlled: yes; only App.kt (font + LaunchedEffect) and PlaybackEngine.ios.kt (MPRemoteCommandCenter).
- Remaining risk: iOS lockscreen widget appearance needs manual runtime validation on a real iOS device/simulator with an active playback session. The compile/test build passes but visual confirmation requires a device.

Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: Clear Library FontWeight.Black + 2 LaunchedEffect blocks
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/PlaybackEngine.ios.kt`: MPRemoteCommandCenter registration, playbackState, playbackRate

Next owner: user for manual iOS lockscreen runtime validation.
Blockers: none for compile/test verification.

## Handoff - 2026-06-26 Dev panel scroll + DrillDownView wiring

Route: openspec+superpowers
Owner: implementation
Scope: Debug why dev panel was missing — the panel was visible but unreachable due to missing scroll on NowPlayingScreen and null currentLibraryTrack in DrillDownView.
Root cause:
- `NowPlayingScreen` Column had `fillMaxSize()` but no `verticalScroll`, so content below the fold (including dev panel) was clipped on smaller screens.
- `DrillDownView` hardcoded `currentLibraryTrack = null` when calling `NowPlayingScreen`, so dev panel never showed from album/artist drill-down.
Implementation:
- `NowPlayingScreen.kt`: added `import androidx.compose.foundation.rememberScrollState` and `import androidx.compose.foundation.verticalScroll`, added `.verticalScroll(rememberScrollState())` to the main Column modifier.
- `App.kt`: added `libraryTracks: List<LibraryTrack>` parameter to `DrillDownView`, resolved `currentLibTrack` from `currentTrack.id`, passed it to `NowPlayingScreen`. Updated both callers (album/artist drill-down in `LibraryHomeScreen`) to pass `libraryTracks`.
Verification:
- `./init.sh`: BUILD SUCCESSFUL — all platforms pass (JVM tests, iOS simulator tests, desktop compile, Android debug APK).
Acceptance:
- Requirement matched: yes; dev panel is now reachable via scroll on NowPlayingScreen and visible from drill-down views.
- Scope controlled: yes; only NowPlayingScreen scrolling and DrillDownView wiring.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for manual visual confirmation.
Blockers: none.

## Handoff - 2026-06-29 Main screen summary removal

Route: implementation
Owner: implementation
Scope: Remove the `RHYTHHAUS` pill and main-screen track count/duration summaries, while keeping album/artist drill-down track-count subtitles.
Implementation:
- `App.kt`: removed the summary text from `HeaderSection`; the main `Library queue` `SectionLabel` now passes `subtitle = null`; `SectionLabel` renders its subtitle only when present, preserving drill-down subtitles.
Verification:
- `./gradlew :shared:jvmTest --configuration-cache`: BUILD SUCCESSFUL. Existing Compose dependency version mismatch warnings were emitted.
Acceptance:
- Requirement matched: yes; the main screen no longer shows `xx tracks · xxx:xx` or `xx tracks • xxx:xx total`, and album/artist drill-down subtitles remain.
- Scope controlled: yes; only shared main-screen header/section-label UI changed.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for optional visual confirmation.
Blockers: none.

## Handoff - 2026-06-29 Drill-down scrollbar travel fix

Route: systematic-debugging
Owner: implementation
Scope: Fix custom album/artist drill-down scrollbar thumb being constrained to the upper right side.
Root cause:
- The scroll indicator used a hard-coded `scrollFraction * 100.dp` offset capped at `200.dp`, so the thumb could not travel across the actual available track height.
Implementation:
- `App.kt`: compute scroll fraction from total vs visible lazy-list items and use `BoxWithConstraints` to offset the thumb across `maxHeight - thumbHeight`.
Verification:
- `./gradlew :shared:jvmTest --configuration-cache`: BUILD SUCCESSFUL. Existing Compose dependency version mismatch warnings were emitted.
Acceptance:
- Requirement matched: yes; scrollbar travel now scales to the full right-side track height instead of a fixed upper band.
- Scope controlled: yes; only the custom drill-down scroll indicator changed.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
Next owner: user for visual confirmation in album/artist track screens.
Blockers: none.

## Handoff - 2026-06-29 Square artwork and swipe back

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Make shared Compose album artwork square and add shared Compose-only left-edge swipe-back to detail/full-screen views.
Input: `docs/superpowers/specs/2026-06-29-square-artwork-swipe-back-design.md` and `docs/superpowers/plans/2026-06-29-square-artwork-swipe-back.md`.
Implementation:
- `App.kt`: made AlbumCard and older inline NowPlayingCard artwork square with `aspectRatio(1f)`, kept decoded artwork cropped, and applied `leftEdgeSwipeBack(onBack)` to `DrillDownView`.
- `NowPlayingScreen.kt`: made full Now Playing artwork square and applied `leftEdgeSwipeBack(onBack)` to the full-screen surface.
- `SwipeBackGesture.kt`: added shared Compose left-edge horizontal drag helper with edge-start and distance thresholds.
Verification:
- Implementer: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL.
- Reviewer first pass: spec rejected for missing `ContentScale.Crop` on older inline NowPlayingCard artwork; quality approved.
- Fix: commit `7c9cdba` added `ContentScale.Crop` to the inline artwork Image only and re-ran `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL.
- Reviewer second pass: spec approved; quality approved; no findings.
- Harness final verification: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 497ms. Existing Compose dependency version mismatch warning remains.
Acceptance:
- Requirement matched: yes; rectangular album art paths are square, compact square/circle artwork remains unchanged, and shared Compose swipe-back applies to album/artist drill-down and full Now Playing.
- Scope controlled: yes; no `iosApp` files, dependencies, Material migration, or native SwiftUI navigation changes.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SwipeBackGesture.kt`
Commits:
- `19fa018` `docs: design square artwork swipe back`
- `1c525b6` `docs: plan square artwork swipe back`
- `5a6898a` `feat: add square artwork and swipe back`
- `7c9cdba` `fix: crop inline now playing artwork`
Next owner: user for visual/manual gesture confirmation on target devices.
Blockers: none.

## Handoff - 2026-06-29 Selectable scrollbar and ripple feedback

Route: openspec+superpowers (subagent-driven)
Owner: implementation
Scope: Make the shared Compose drill-down scrollbar selectable and add press feedback to main visible custom clickables/lists.
Input: `docs/superpowers/specs/2026-06-29-selectable-scrollbar-ripple-design.md` and `docs/superpowers/plans/2026-06-29-selectable-scrollbar-ripple.md`.
Implementation:
- `HausClickable.kt`: added `Modifier.hausClickable(onClick)` using foundation `clickable`, remembered `MutableInteractionSource`, and `LocalIndication.current` to avoid Material/Material3 imports.
- `App.kt`: replaced approved visible custom clickables with `hausClickable` and replaced the visual-only drill-down scrollbar with a right-edge 24 dp tap/drag scrubber and 6 dp thumb.
- `NowPlayingBar.kt`, `NowPlayingScreen.kt`, `SearchScreen.kt`, and `SettingsScreen.kt`: applied `hausClickable` to visible custom controls/lists while preserving invisible overlay blockers.
Verification:
- Initial implementer timed out after partial edits; controller inspected state and ran `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 453ms.
- Recovery implementer: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 578ms; committed `5a04ac0`.
- Task reviewer: spec approved; quality approved; no Critical/Important findings. Minor note: manual visual/device verification was not performed, and `LocalIndication.current` was accepted as satisfying visible press feedback without Material/Material3 imports.
- Harness final verification: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL in 414ms. Existing Compose dependency version mismatch warning remains.
Acceptance:
- Requirement matched: yes; scrollbar hit target is wider and selectable via tap/drag, and main visible custom clickables use shared press feedback.
- Scope controlled: yes; changes are limited to shared Compose files, with no iOS/platform/dependency/version catalog changes.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/HausClickable.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
Commits:
- `959b975` `docs: design selectable scrollbar ripple feedback`
- `7aaa584` `docs: plan selectable scrollbar ripple feedback`
- `5a04ac0` `feat: add selectable scrollbar and ripple feedback`
Next owner: user for manual visual confirmation on target devices.
Blockers: none.

## Handoff - 2026-06-29 Unified platform version metadata

Route: openspec+superpowers
Owner: implementation
Scope: Make root `gradle.properties` the single editable source for app version name/code across Android, desktop/macOS, and iOS.
Input: `docs/superpowers/specs/2026-06-29-unified-platform-version-design.md` and `docs/superpowers/plans/2026-06-29-unified-platform-version.md`.
Implementation:
- `gradle.properties`: added `rhythhaus.versionName=1.0.0` and `rhythhaus.versionCode=1`.
- `androidApp/build.gradle.kts`: reads Gradle properties for `versionName` and integer `versionCode`.
- `desktopApp/build.gradle.kts`: reads the same version name for Compose Desktop `packageVersion`.
- `build.gradle.kts`: added cacheable `syncIosVersionXcconfig` task to write iOS version settings from Gradle properties.
- `iosApp/Configuration/Config.xcconfig` and `Version.xcconfig`: map Xcode `MARKETING_VERSION` and `CURRENT_PROJECT_VERSION` from the synced version keys.
Verification:
- `./gradlew syncIosVersionXcconfig --configuration-cache`: BUILD SUCCESSFUL; wrote valid xcconfig syntax.
- `./gradlew syncIosVersionXcconfig :androidApp:assembleDebug :desktopApp:compileKotlin --configuration-cache`: BUILD SUCCESSFUL in 12s. Existing Compose dependency mismatch/deprecation warnings remain.
- `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION'`: resolved `MARKETING_VERSION = 1.0.0`, `CURRENT_PROJECT_VERSION = 1`, `RHYTHHAUS_VERSION_NAME = 1.0.0`, and `RHYTHHAUS_VERSION_CODE = 1`.
Acceptance:
- Requirement matched: yes; changing only root `gradle.properties` version keys controls Android, desktop/macOS, and iOS version metadata after the sync task updates the committed xcconfig.
- Scope controlled: yes; no app IDs, signing settings, deployment targets, SDK/plugin/dependency versions, or packaging scope changed.
Changed files:
- `gradle.properties`
- `build.gradle.kts`
- `androidApp/build.gradle.kts`
- `desktopApp/build.gradle.kts`
- `iosApp/Configuration/Config.xcconfig`
- `iosApp/Configuration/Version.xcconfig`
Next owner: user for future version bumps by editing `gradle.properties` and running `./gradlew syncIosVersionXcconfig`.
Blockers: none.

## Handoff - 2026-06-29 iOS target version/build wiring

Route: openspec+superpowers follow-up
Owner: implementation
Input: User noted the unified version metadata must also update the iOS target's Version and Build fields.
Output:
- `iosApp/iosApp.xcodeproj/project.pbxproj`: Debug and Release target build settings now set `CURRENT_PROJECT_VERSION = "$(RHYTHHAUS_VERSION_CODE)"`, `INFOPLIST_KEY_CFBundleShortVersionString = "$(MARKETING_VERSION)"`, and `INFOPLIST_KEY_CFBundleVersion = "$(CURRENT_PROJECT_VERSION)"`.
Verification:
- `/usr/bin/xcrun xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -showBuildSettings | grep -E 'MARKETING_VERSION|CURRENT_PROJECT_VERSION|RHYTHHAUS_VERSION|INFOPLIST_KEY_CFBundleShortVersionString|INFOPLIST_KEY_CFBundleVersion'`: resolved `MARKETING_VERSION = 1.0.0`, `CURRENT_PROJECT_VERSION = 1`, `INFOPLIST_KEY_CFBundleShortVersionString = 1.0.0`, `INFOPLIST_KEY_CFBundleVersion = 1`, and the shared `RHYTHHAUS_VERSION_*` values.
- `./gradlew syncIosVersionXcconfig --configuration-cache`: BUILD SUCCESSFUL.
Next owner: user for future version bumps in `gradle.properties`; run `./gradlew syncIosVersionXcconfig` before opening/releasing from Xcode if the xcconfig is stale.
Blockers: none.

## Handoff - 2026-07-06 library scroll bar visibility

Route: openspec+superpowers
Owner: implementation
Scope: Scroll direction controls `NowPlayingBar` visibility on every scrollable screen that renders a bar: Home Library list, Search results, and album/artist track-list drill-down screens.
Implementation:
- Added pure common `LibraryScrollPosition` and `decideNowPlayingBarVisibilityForLibraryScroll` helper with tests for same-item down/up, item-boundary down/up, and jitter no-op behavior.
- Wired Home, Search results, and album/artist `DrillDownView` list scroll states to a hoisted `isNowPlayingBarVisible` state using the tested helper.
- Rendered both root fixed and drill-down `NowPlayingBar` paths through bottom `AnimatedVisibility` while preserving existing bar callbacks and Now Playing overlay.
Reviews:
- Task 1 independent review: clean; no Critical, Important, or Minor findings.
- Task 2 initial independent review: clean for the original Home-only scope.
- Widened-scope independent review: clean; no Critical or Important findings. One Minor EOF whitespace finding in `App.kt` was already fixed before staging and rechecked with `git diff --check` / `git diff --cached --check`.
Verification:
- `openspec validate library-scroll-bar-visibility --strict`: pass (`Change 'library-scroll-bar-visibility' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 988ms`; 24 actionable tasks: 7 executed, 17 up-to-date; configuration cache reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 98 actionable tasks: 12 executed, 86 up-to-date). Existing Android deprecation warning for `MediaMetadata.Builder.setArtworkData` remains unrelated.
- `/usr/bin/xcrun xcodebuild -version`: Xcode 26.6, Build version 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 15s`; 33 actionable tasks: 8 executed, 25 up-to-date). Existing iOS test warnings remain unrelated.
- `git diff --check`: pass (no output, exit 0) after trimming a trailing blank line in `App.kt`.
Acceptance:
- Requirement matched: yes at source/automated-verification level — Home, Search results, and album/artist track lists now hide on downward scroll, show on upward scroll, preserve jitter no-op, bottom animation, and no pointer interception after hidden `AnimatedVisibility` exit.
- Scope controlled: yes — no new dependencies, native navigation changes, route-stack changes, playback/scanner/library/theme/platform changes, or changes to Now Playing content layout.
- Edge cases/risk reviewed: automated checks verify helper behavior and compilation; subjective animation feel and live gesture/hit-test behavior still need optional manual visual validation on device/simulator.
Changed files:
- `docs/superpowers/specs/2026-07-06-library-scroll-bar-visibility-design.md`
- `docs/superpowers/plans/2026-07-06-library-scroll-bar-visibility.md`
- `openspec/changes/library-scroll-bar-visibility/proposal.md`
- `openspec/changes/library-scroll-bar-visibility/design.md`
- `openspec/changes/library-scroll-bar-visibility/specs/library-navigation/spec.md`
- `openspec/changes/library-scroll-bar-visibility/tasks.md`
- `openspec/changes/library-scroll-bar-visibility/.openspec.yaml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `progress.md`
Next owner: user/OpenSpec for manual visual validation and archive when satisfied.
Blockers: none for automated validation.
Commit: pending semantic commit after staged-diff review/approval.



## Handoff - 2026-07-06 playback repeat shuffle

Route: openspec+superpowers
Owner: implementation
Scope: Shared playback repeat/shuffle modes and NowPlayingScreen controls.
Implementation:
- Added shared RepeatMode/ShuffleMode state and controller APIs.
- Centralized completion, previous, and next through mode-aware effective order logic.
- Added shuffle effective order generation that preserves current track and keeps visible library order unchanged.
- Added NowPlayingScreen repeat/shuffle controls using Material vector icons.
- Stabilized controller mode tests by isolating state-machine assertions from asynchronous fake-engine callbacks after broad verification exposed an order-dependent race.
Verification:
- openspec validate playback-repeat-shuffle --strict: pass (`Change 'playback-repeat-shuffle' is valid`).
- ./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.PlaybackControllerTest' --configuration-cache: pass after deterministic test hardening (`BUILD SUCCESSFUL in 1s`).
- ./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache: first two broad runs failed in `PlaybackControllerTest.disablingShuffleReturnsToOriginalQueueOrderFromCurrentTrack` because async fake-engine callbacks raced controller state assertions; exact focused reruns passed, tests were made deterministic in `cbfcfdc`, and final broad rerun passed (`BUILD SUCCESSFUL in 2s`).
- /usr/bin/xcrun xcodebuild -version: Xcode 26.6, Build version 17F113.
- ./gradlew :shared:iosSimulatorArm64Test --configuration-cache: pass (`BUILD SUCCESSFUL in 18s`).
- git diff --check: pass before evidence edits; will be rerun after evidence commit.
Acceptance:
- Requirement matched: yes — repeat modes, shuffle modes, controller navigation semantics, and NowPlayingScreen controls match the approved plan at source/test level.
- Scope controlled: yes — no dependency changes; no mini-player/system-notification controls; visible library/browse order remains unchanged.
- Edge cases/risk reviewed: stop-after-current keeps current track and stops at duration; stop-after-queue stops at final effective track without wrapping; repeat-playlist wraps; repeat-one loops automatically while manual transport remains adjacent; shuffle enable/disable and shuffled queue replacement covered by tests. Manual playback UX validation on device/simulator was not performed in this CLI session.
Changed files:
- docs/superpowers/specs/2026-07-06-playback-repeat-shuffle-design.md
- docs/superpowers/plans/2026-07-06-playback-repeat-shuffle.md
- openspec/changes/playback-repeat-shuffle/.openspec.yaml
- openspec/changes/playback-repeat-shuffle/proposal.md
- openspec/changes/playback-repeat-shuffle/design.md
- openspec/changes/playback-repeat-shuffle/specs/audio-playback/spec.md
- openspec/changes/playback-repeat-shuffle/tasks.md
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Playback.kt
- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt
- shared/src/commonTest/kotlin/com/eterocell/rhythhaus/PlaybackControllerTest.kt
- progress.md
Next owner: user/OpenSpec for manual playback UX validation and archive when satisfied.
Blockers: none for automated validation. Unrelated untracked `roadmap.md` remains untouched.
Commit:
- 4c2974e docs: spec playback repeat shuffle
- 5f1225a feat: add playback mode state
- ac59554 feat: add repeat and shuffle queue navigation
- 0c6f394 fix: preserve loading state during auto play transitions
- 906a00e feat: add now playing repeat shuffle controls
- cbfcfdc test: stabilize playback controller mode tests
- Evidence/docs finalization commit pending.


## Handoff - 2026-07-06 roadmap items 2-4

Route: mixed — systematic-debugging for roadmap bugfixes, openspec+superpowers for Nested Scroll/Haze UI feature.
Owner: implementation
Input: roadmap.md unfinished items in order.
Output:
- Fixed NowPlayingBar re-enter animation after returning from NowPlayingScreen by keeping the fixed bottom bar in composition and animating scroll hide/show with offset/alpha instead of gating it on `showNowPlaying`.
- Fixed compact Miuix button descender clipping risk by reducing inside vertical margin on fixed-height 36dp/40dp text buttons so glyphs such as `g` have enough vertical content space.
- Added Nested Scroll/Haze feature for Library/Home and album/artist track-list pages: pure tested `NestedScrollChromeState`, Haze dependency (`dev.chrisbanes.haze:haze`), `hazeSource` on lists, and top `hazeEffect` chrome driven by list scroll progress.
Changed files:
- `roadmap.md`
- `gradle/libs.versions.toml`
- `shared/build.gradle.kts`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `docs/superpowers/specs/2026-07-06-nested-scroll-blur-design.md`
- `docs/superpowers/plans/2026-07-06-nested-scroll-blur.md`
- `openspec/changes/nested-scroll-blur/*`
Verification:
- `openspec validate nested-scroll-blur --strict` -> `Change 'nested-scroll-blur' is valid`.
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` first failed red because `nestedScrollChromeStateFor` was missing, then passed after implementation.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` -> `BUILD SUCCESSFUL` after Haze dependency/API integration.
Next owner: implementation can continue with roadmap item 5 (`i18n`) via OpenSpec/Superpowers.
Blockers: none for JVM/common verification. Manual visual validation on Android/iOS/desktop not performed in this session.


## Handoff - 2026-07-06 i18n

Route: openspec+superpowers
Owner: implementation
Input: roadmap.md item 5 (`i18n`).
Output:
- Added OpenSpec change `openspec/changes/i18n/` and Superpowers design/plan docs for shared Compose i18n.
- Added Compose Multiplatform string resources under `shared/src/commonMain/composeResources/values/strings.xml` and `values-zh/strings.xml`.
- Migrated shared Compose UI text/content descriptions in BackChip, NowPlayingBar, NowPlayingScreen, SearchScreen, SettingsScreen, and primary App.kt UI paths to `stringResource(Res.string.*)` while leaving user media metadata unchanged.
- Marked roadmap i18n item complete.
Changed files include:
- `roadmap.md`
- `progress.md`
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonMain/composeResources/values-zh/strings.xml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/BackChip.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingBar.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/NowPlayingScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SearchScreen.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`
- `docs/superpowers/specs/2026-07-06-i18n-design.md`
- `docs/superpowers/plans/2026-07-06-i18n.md`
- `openspec/changes/i18n/*`
Verification:
- `openspec validate i18n --strict` -> `Change 'i18n' is valid`.
- `./gradlew :shared:compileKotlinJvm --configuration-cache` after resource creation -> `BUILD SUCCESSFUL`.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` after code migration -> `BUILD SUCCESSFUL`.
Next owner: final verification/review; roadmap has no remaining unchecked item.
Blockers: none for shared JVM verification. Manual locale visual QA not performed.


## Handoff - 2026-07-06 nested scroll bugfix

Route: systematic-debugging (bugfix)
Owner: implementation
Input: user reported Nested Scroll effect covers nearly all Library screen, transition is not smooth, duplicate large heading + toolbar title visible simultaneously, and toolbar does not cover iOS status bar.
Root cause:
- `NestedScrollBlurChrome` used a fixed 92dp layer after `statusBarsPadding()` plus a negative `headerOffsetPx`, making the Haze/scrim area visually too large and unstable.
- Toolbar title alpha followed raw scroll progress from 0, so it appeared while the large page heading was still visible.
- Status bar coverage depended on a padded box height rather than a stable status-bar + toolbar structure.
Fix:
- Made default nested-scroll header offset zero and shortened activation distance to 96px for a faster, less jumpy transition.
- Reworked `NestedScrollBlurChrome` into a status-bar-covering container plus a fixed 56dp toolbar area.
- Reduced Haze blur/tint/scrim intensity and delayed toolbar title fade-in until scroll progress is ~68%, preventing simultaneous prominent headings.
Verification:
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` -> `BUILD SUCCESSFUL`.
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` -> `BUILD SUCCESSFUL`.
- `git diff --check && openspec validate nested-scroll-blur --strict` -> no diff-check output, `Change 'nested-scroll-blur' is valid`.
Next owner: user for manual visual check on iOS/Library screen; implementation can tune thresholds/height further if needed.
Blockers: none.


## Handoff - 2026-07-06 nested scroll blur root-cause fix

Route: systematic-debugging (bugfix)
Owner: implementation
Input: User reported the Haze/blur nested-scroll chrome still covers nearly all of the Library screen after scrolling, contradicting the prior (unverified) parameter-tuning fix.

Phase 1 root cause investigation:
- Built and ran the real iOS app in the iPhone 17 (iOS 26.5) simulator via `xcodebuild` + `xcrun simctl`, not just static code reading.
- Reproduced the exact user-reported symptom: after scrolling, screenshots showed the content grid (album thumbnails + captions) uniformly blurred well below the intended 56dp toolbar, while status bar and toolbar title stayed sharp.
- Quantified this objectively (not just visual impression) with a Laplacian-variance sharpness metric on the same album cover crop before/after scroll: 1129 (sharp, unscrolled) -> 41 (heavily blurred, scrolled), a ~96% sharpness loss confirming a real blur filter was drawn over content far outside the chrome box.
- Isolated the Haze effect specifically via a red-color-box substitution experiment to separate "my Box layout is oversized" from "Haze itself draws past its host bounds" hypotheses.
- Traced Haze 1.7.2 library source (`HazeEffectNode.kt`, `BlurEffect.kt`) and found the actual root causes in `NestedScrollBlurChrome`:
  1. `Modifier.statusBarsPadding().height(0.dp)` inside the haze-effect Box squeezed the status-bar inset to zero instead of reserving space for it (this explains "does not cover iOS status bar").
  2. The Haze `hazeEffect` call had no explicit `clipToAreasBounds`, relying on Haze's implicit heuristic (`backgroundColor.alpha <= 0.9f` triggers clip) with alpha 0.92 sitting right at that fragile boundary, and the chrome Box had no fixed total height, so its measured height (and thus the Haze layer bounds) was not reliably constrained to the toolbar strip.
- Fix implemented: compute `chromeHeight = statusBarInset + 56dp` explicitly via `WindowInsets.statusBars.asPaddingValues().calculateTopPadding()`, apply it as a single `Modifier.height(chromeHeight)` on the outer chrome Box, and pass `clipToAreasBounds = true` / `blurEnabled = true` explicitly to `hazeEffect` instead of relying on the alpha heuristic.
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache` -> BUILD SUCCESSFUL.
- `./gradlew :shared:jvmTest --configuration-cache` -> BUILD SUCCESSFUL (all LibraryNavigationTest cases pass, including nested-scroll-chrome tests).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL.
- iOS: rebuilt via `xcodebuild ... build` (BUILD SUCCEEDED) and reinstalled/relaunched on iPhone 17 simulator (iOS 26.5) to confirm the app runs with the fix. The GUI automation driver (computer_use) that was used to reproduce the bug lost its session mid-task and could not be revived with the tools available in this session, so a fresh visual/AX-tree re-confirmation of "chrome no longer covers most of the screen after scrolling" on-device was not completed after the fix landed — this is an honest gap, not claimed as done.
Changed files (this session, in addition to prior nested-scroll-blur / i18n work):
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt` (NestedScrollBlurChrome rewritten; new WindowInsets imports)
Next owner: user (or a future session with a working computer_use/simulator driver) to visually re-confirm on iOS/Android/desktop that the chrome is now bounded to the toolbar strip and covers the status bar.
Blockers: none for automated/JVM verification. On-device visual re-confirmation after the fix is the one gap — GUI driver session died and could not be restarted with available tools.


## Handoff - 2026-07-06 drop Haze from nested scroll chrome

Route: systematic-debugging (bugfix follow-up, user-directed)
Owner: implementation
Input: user asked to drop Haze from the nested-scroll chrome and use a plain (unblurred) scrim instead, pending their own visual check.
Output:
- Removed `hazeSource`/`hazeEffect`/`HazeState` usage from `NestedScrollBlurChrome` and its two call sites (Library home list, DrillDownView track list) in App.kt.
- `NestedScrollBlurChrome` now renders a fixed-height (status bar inset + 56dp toolbar) plain color scrim instead of a blurred Haze layer; scrim opacity still scales with scroll progress.
- Removed the `dev.chrisbanes.haze:haze` dependency entirely: `gradle/libs.versions.toml` (version + library entries) and `shared/build.gradle.kts` (`implementation(libs.haze)`).
- Marked roadmap item 4 (nested scroll + blur) back to `[ ]` / WIP since the blur requirement is currently dropped pending the user's own visual confirmation of the plain-scrim look.
Verification:
- `./gradlew :shared:compileKotlinJvm :shared:jvmTest --configuration-cache` -> BUILD SUCCESSFUL.
- `./gradlew :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> BUILD SUCCESSFUL.
- iOS: `xcodebuild ... build` -> BUILD SUCCEEDED, reinstalled/relaunched on iPhone 17 simulator (iOS 26.5), app launches and Library screen renders.
- NOT verified: live on-device scroll behavior of the new plain-scrim chrome. The computer_use GUI automation driver used earlier in this session to reproduce/diagnose the Haze bug lost its session and could not be revived with the tools available here, so a fresh screenshot-based confirmation of the post-Haze-removal scroll chrome was not completed. This is an open item for the user or a future session with a working driver.
Next owner: user, to visually confirm the plain-scrim nested-scroll chrome looks acceptable; if so, mark roadmap item 4 done, if not, decide follow-up (different blur approach, or keep plain scrim as final design).
Blockers: none for build verification. On-device visual confirmation is pending.


## Handoff - 2026-07-07 architecture refactor

Route: openspec+superpowers
Owner: implementation
Input: architecture-refactor spec/plan
Output: behavior-preserving shared Compose architecture refactor: pure navigation decisions, LibraryAppState coordinator, route/adaptive shell extraction, home/detail content extraction, and chrome/dialog/row component split.
Verification:
- `openspec validate architecture-refactor --strict`: pass (`Change 'architecture-refactor' is valid`).
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 404ms`; 25 actionable tasks: 4 executed, 21 up-to-date; configuration cache entry reused).
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 7s`; 99 actionable tasks: 14 executed, 85 up-to-date; configuration cache entry reused). Existing Android warning only: `PlaybackEngine.android.kt:252:17 'fun setArtworkData(p0: ByteArray?): MediaMetadata.Builder' is deprecated`.
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 15s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache entry reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest.kt` about unnecessary non-null assertions/no casts needed.
- `git diff --check`: pass (no output, exit 0).
- App.kt line count after split: `166 shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`.
Changed files:
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryNavigation.kt`
- `shared/src/commonTest/kotlin/com/eterocell/rhythhaus/LibraryNavigationTest.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppState.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryAppShell.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRoutes.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryHomeContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDetailContent.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryChrome.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryDialogs.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/LibraryRows.kt`
- `docs/superpowers/specs/2026-07-07-architecture-refactor-design.md`
- `docs/superpowers/plans/2026-07-07-architecture-refactor.md`
- `openspec/changes/architecture-refactor/proposal.md`
- `openspec/changes/architecture-refactor/design.md`
- `openspec/changes/architecture-refactor/specs/app-architecture/spec.md`
- `openspec/changes/architecture-refactor/tasks.md`
- `progress.md`
Reviews:
- Task 1 review: changes requested for no-op pop/root transitions; fixed and re-reviewed clean.
- Task 2 review: clean after timeout salvage and composition-time mutation fix.
- Task 3 review: clean.
- Task 4 review: clean after timeout salvage and scope correction.
- Task 5 review: clean.
Next owner: user for manual visual smoke validation of library navigation/detail/search/settings/clear-library routes on target devices; OpenSpec/user for archive when satisfied.
Blockers: none for automated OpenSpec/JVM/desktop/Android/iOS verification. Manual visual validation remains recommended because this was a behavior-preserving UI extraction.
Commits: `7c7e895`, `aafa446`, `a6c78e7`, `80e0d8f`, `e37470f`; final evidence commit pending.

## Handoff - 2026-07-07 i18n completeness

Route: openspec+superpowers
Owner: implementation
Input: existing i18n design spec/plan (`docs/superpowers/specs/2026-07-06-i18n-design.md`, `docs/superpowers/plans/2026-07-06-i18n.md`)
Scope: Complete missing shared Compose UI string localization and remove dead resource keys.
Findings:
- Existing `composeResources/values/strings.xml` and `values-zh/strings.xml` were in sync but missing keys for several user-facing strings.
- `ImportLabels` used platform-specific `expect val` hardcoded strings; migrated to common composable functions using string resources and deleted Android/JVM/iOS actual files.
Fix:
- Added 12 new localized keys to both English and Chinese resource files: `adaptive_detail_placeholder`, `browse_mode_albums`, `browse_mode_artists`, `browse_mode_songs`, `import_card_title`, `import_card_title_with_tracks`, `import_card_description`, `scan_complete_format`, `folder_picker_error_access`, `folder_picker_error_select`, `folder_picker_error_prepare`, `folder_picker_no_folder_selected`.
- Removed 8 unused/dead keys from both files: `album_count_format`, `now_playing_label`, `pause_playback`, `play_selected_track`, `playback_seek_position`, `playback_status_format`, `stop`, `stop_playback`.
- Replaced hardcoded browse-mode labels in `LibraryRows.kt` with `stringResource(mode.displayLabelResource())` and a private `BrowseMode.displayLabelResource()` extension.
- Replaced hardcoded adaptive placeholder in `LibraryAppShell.kt` with `stringResource(Res.string.adaptive_detail_placeholder)`.
- Replaced hardcoded scan-complete message in `App.kt` with the new `scan_complete_format` resource, using `.replaceFirst` for Kotlin/Multiplatform integer substitution.
- Localized platform folder-picker failure/unavailable messages in `PlatformSourceAccess.android.kt`, `.jvm.kt`, and `.ios.kt` by resolving `stringResource` values in the composable launcher and passing them into `PlatformFolderPickResult`.
Changed files:
- `shared/src/commonMain/composeResources/values/strings.xml`
- `shared/src/commonMain/composeResources/values-zh/strings.xml`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ImportLabels.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryRows.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt`
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/library/PlatformSourceAccess.ios.kt`
- `docs/superpowers/plans/2026-07-06-i18n.md`
- `openspec/changes/i18n/tasks.md`
Deleted files:
- `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/ImportLabels.android.kt`
- `shared/src/jvmMain/kotlin/com/eterocell/rhythhaus/ImportLabels.jvm.kt`
- `shared/src/iosMain/kotlin/com/eterocell/rhythhaus/ImportLabels.ios.kt`
Verification:
- `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :shared:jvmTest --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :desktopApp:compileKotlin --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `./gradlew :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL`).
- `git diff --check`: pass (no output).
Next owner: user for manual visual validation of localized strings in English and Chinese on target devices; OpenSpec for archive when satisfied.
Blockers: none.
Commit: not created; will describe staged diff before committing per user preference.
## Handoff - 2026-07-16 artwork topbar solid correction

Route: openspec+superpowers
Owner: implementation
Input: User superseded artwork topbar Miuix blur with a progressive solid background and requested bottom-bar behavior remain/revert unchanged.
Output: Artwork sticky chrome now uses clamped `HausColors.paper` opacity directly inside the sticky lazy item; artwork blur, measured sibling overlay, and placement state were removed. `NowPlayingBar.kt` has no diff and existing backdrop wiring is unchanged. Focused tests/compiler, strict OpenSpec, diff hygiene, isolated playback rerun, and fresh JVM/desktop/Android matrix pass. Production compact/wide artwork routes, back navigation, wide no-artwork Miuix route, CJK accessibility text, and bottom-bar presence were exercised through Orca.
Next owner: user/manual visual acceptance for partial and pinned solid-background pixels, then OpenSpec final acceptance/archive if desired.
Blockers: Orca could not drive the final custom detail scrollbar/wheel path; iOS tests remain blocked by unchanged `AppScanCancellationTest.kt` JVM-only `Thread` references at lines 64 and 340. Kotlin LSP remains unavailable by prior user choice.

Acceptance update: user manually confirmed progressive solid fade at partial/pinned artwork states and unchanged bottom-bar rendering. Final spec PASS, quality APPROVED, and Oracle PASS were collected. OpenSpec Tasks 5.3 and 5.4 are complete. Remaining blocker applies only to iOS test compilation, not this requested implementation.
