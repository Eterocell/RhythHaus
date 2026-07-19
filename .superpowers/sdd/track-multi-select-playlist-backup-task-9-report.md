# Track Multi-Select Playlist Backup — Task 9 Report

## Scope and commits

- Route: `openspec+superpowers` / Task 9 integration, verification, runtime-QA attempt, and final review.
- Input: `.superpowers/sdd/task-9-brief.md`, approved OpenSpec change, and accepted Tasks 1-8 through `4462380`.
- Integration commit: `0f48f45 test: verify playlist backup integration`.
- Review-fix commit: `912e6e4 fix: bound iOS playlist backup reads`.
- No archive or push was performed. No schema, migration, dependency, toolchain, cloud-sync, raw-database-backup, unsupported-platform, or unrelated playback change was introduced.

## Real JVM/SQLDelight integration acceptance

`PlaylistBackupIntegrationJvmTest` uses one temporary file-backed `LibraryDatabase` with deterministic timestamp and ID factories. It verifies:

- selection order is reconstructed from current visible order and duplicate visible IDs do not duplicate selection submission;
- generalized picker append and inline-create payloads persist through the real repository in unchanged order;
- canonical export is byte-deterministic, preserves duplicate occurrences, decodes successfully, and uses the required ordered logical fields and lowercase eight-digit CRC32;
- encoded bytes exclude local IDs, paths, source/audio/artwork fields and fixture values, scan state, and timestamps;
- import planning produces unique, unmatched, and ambiguous outcomes, skips the no-restorable playlist, and preserves ordered duplicate matches;
- stale confirmation makes zero repository calls and leaves both playlist count and direct total `playlist_entry` row count unchanged;
- a trigger-forced second-playlist entry failure rolls back every new playlist and entry, leaves the direct total `playlist_entry` count equal to its pre-failure value, and preserves every existing row;
- fresh confirmation invokes one all-or-none repository mutation, restores both eligible playlists, preserves duplicate order/positions/distinct occurrence IDs, and leaves the original conflicting playlist unchanged;
- a repeated plan/import deterministically chooses `(Imported 2)` names.

The first focused execution passed. A task-scoped review requested stronger real picker persistence and canonical-wire assertions; those assertions were added without weakening the test, and the forced rerun passed (`BUILD SUCCESSFUL in 8s`, 26/26 tasks executed). No production defect was exposed by the integration test.

## Automated verification

- Complete focused Task 1-8 JVM matrix plus integration, forced: pass, `BUILD SUCCESSFUL in 14s`; 35/35 tasks executed. This included selection/navigation/semantics, generalized picker, codec, matcher, service, repository contract and real SQLDelight repository, JVM document adapter, Settings/workflow/dialog semantics, cancellation/revision ownership, and the new integration test.
- Focused Android-host document adapter, forced: pass, `BUILD SUCCESSFUL in 7s`; 52/52 tasks executed.
- `openspec validate track-multi-select-playlist-backup --strict`: pass; exact output `Change 'track-multi-select-playlist-backup' is valid`.
- `./gradlew :shared:jvmTest :shared:testAndroidHostTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass, `BUILD SUCCESSFUL in 7s`; 125 actionable tasks, 6 executed and 119 up-to-date. No playback flake occurred in this Task 9 full matrix.
- `/usr/bin/xcrun xcodebuild -version`: pass; Xcode 26.6, build 17F113.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass, `BUILD SUCCESSFUL in 36s`; 35 actionable tasks, 14 executed and 21 up-to-date. This is automated simulator-test evidence, not runtime UI acceptance.
- Native `PlaylistBackupDocumentPoliciesTests`: initially 7/7 pass; after the final review fix, 8/8 pass with zero failures.
- Unsigned clean generic-simulator Xcode build: `** BUILD SUCCEEDED **` before and after the review fix.
- `GIT_MASTER=1 git diff --check`: pass with no output before evidence editing.
- Kotlin LSP diagnostics were unavailable because `kotlin-ls` installation was previously declined. Gradle/Kotlin and Xcode/Swift compilation/tests are the executable diagnostics.

## Final-review defect and strict RED/GREEN fix

The change-wide quality/security review found one Important issue: iOS `readBounded` made one `FileHandle.read(upToCount:)` call, so a legal short read before EOF could prematurely accept an actually oversized document.

- RED native XCTest supplied chunks of 3 then 3 bytes for a 4-byte limit. The old policy failed exactly as expected: `XCTAssertThrowsError failed: did not throw an error`, and the handle recorded only `[5]`.
- GREEN changed only the iOS resource policy to accumulate actual returned bytes until EOF or `maxBytes + 1`, requesting only remaining capacity. The regression passed, then all 8 native policy tests passed.
- Post-fix full iOS simulator, combined JVM/Android-host/desktop/Android, unsigned Xcode build, and diff-hygiene gates passed.
- Final adjudication: PASS. Unreturned `read(upToCount:)` capacity is not consumed data; actual accumulated bytes stop at 4 MiB + 1. No Critical or Important finding remains.

Non-blocking review notes retained: adapter result objects can carry platform exception text although shared UI maps it to generic errors; direct SQL trigger/count assertions are intentionally SQLite-specific test code; ambiguous candidate diagnostic ordering follows destination order. None changes accepted product behavior or is a Task 9 blocker.

## Runtime and visual QA attempt

### Desktop JVM/macOS

- `:desktopApp:run` launched Java process `MainKt` PID 74386 with a visible `RhythHaus` window at 784×588.
- Orca runtime was ready and Accessibility/Screenshot permissions reported granted, but `get-app-state` returned `permission_denied`: the visible Java/Compose app exposed no accessibility window.
- Native window-ID capture failed with `could not create image from window`. A full-screen PNG was created at `/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-task9-screen.png`, but the available model/tool path could not render it.
- Therefore no desktop long-press, tap, checkbox, accessible selection action, current-track selection, Back/navigation/Search cleanup, bottom-bar clearance/hit target, picker dismissal/failure/success, Settings panel, import/export, focus, or pixel result is claimed.

### iOS

- The unsigned Debug app was installed and launched in the iPhone 17 Pro simulator (app PID 75202).
- Simulator had a visible 448×954 window but likewise exposed no accessibility window to Orca.
- A real simulator framebuffer PNG was captured at `/var/folders/l_/j8p3d1ln6q1drdptb1hhczrh0000gn/T/opencode/rhythhaus-task9-ios.png`; the available model/tool path could not render it.
- Therefore no iOS touch, selection, picker presentation, Settings panel, import/export, focus/accessibility order, or pixel result is claimed.

### Android

- `adb devices -l` started the daemon and reported no attached device or emulator.
- No Android runtime, SAF panel, touch, accessibility, or visual result is claimed.

### Visual QA verdict

The visual-qa workflow was attempted but could not reach its screenshot-review prerequisite. No image diff or dual visual pass was validly runnable because no reference baseline was provided and captured pixels could not be rendered by the available review path. Compact/wide, light/dark, English/Chinese and CJK metrics, row/checkbox fit, contextual-bar clearance and mutual exclusion, focus order, long preview late rows, issue presentation, dialog/panel/scrim appearance, and platform touch behavior remain unverified—not passed.

## Reviews and OpenSpec status

- Task-scoped integration review: initial REVISE for assertion strength; addressed; final automated acceptance PASS.
- Final goal/spec review: PASS; Task 8.1 fully met, no Critical or Important finding.
- Final quality/security review: initial REVISE for iOS short reads; strict RED/GREEN fix committed; final contract adjudication PASS with no Critical or Important finding.
- QA evidence audit: automated Task 8.1-8.3 may close; runtime/visual Task 9.1-9.3 must remain open.
- OpenSpec 8.1, 8.2, 8.3, and 9.4 are complete. OpenSpec 9.1, 9.2, and 9.3 remain unchecked because runtime interaction, real system-panel presentation, and visual acceptance were unavailable.

Next owner: user/manual QA or a future session with attachable desktop/mobile accessibility plus renderable screenshots. Archive only on an explicit later request.

Blockers: runtime interaction/system-panel and visual acceptance only. No integration, focused test, strict OpenSpec, supported build/test matrix, native XCTest, Xcode build, diff-hygiene, or final source-review blocker.
