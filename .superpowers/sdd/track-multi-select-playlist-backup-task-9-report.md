# Track Multi-Select Playlist Backup — Task 9 Report

## Scope and commits

- Route: `openspec+superpowers` / Task 9 integration, verification, runtime-QA attempt, and final review.
- Input: `.superpowers/sdd/task-9-brief.md`, approved OpenSpec change, and accepted Tasks 1-8 through `4462380`.
- Integration commit: `0f48f45 test: verify playlist backup integration`.
- Review-fix commit: `912e6e4 fix: bound iOS playlist backup reads`.
- No archive or push was performed. No schema, migration, dependency, toolchain, cloud-sync, raw-database-backup, unsupported-platform, or unrelated playback change was introduced.

## Review correction in progress

The final controller review reopened OpenSpec 8.1, 8.2, and 9.4 because the privacy proof used representative forbidden values instead of a distinctive canary for every seeded local-only field and the focused-test evidence recorded Gradle task counts rather than exact executed-test and skip counts. Commit `5741a4d5233ac1f5b0ec3a70aa7a259153bb5cbc` addresses the test finding, and evidence-correction commit `6af0ba85e629819937fcee60dad6d9909234ee45` addresses the count/lifecycle finding. OpenSpec 8.1-8.3 and 9.4 are complete; 9.1-9.3 remain open and unverified.

### Exhaustive encoded-byte privacy proof

- Each of the five exported tracks now has a distinct source and distinct canaries for track ID, source ID, source display name, source handle/path, source created timestamp, source-local key, audio-source path/identifier, track display name/path, size, modified/created/updated timestamps, scan ID, artwork MIME, raw searchable artwork text, and Base64 artwork payload: 16 canaries per entry, 80 assertions total.
- The alpha scan canary is exactly `private-scan`; the other four scan values are separately distinctive. Every exported entry is covered rather than a representative subset.
- Positive portable-wire assertions remain for canonical format/key order, playlist names/order, title, artist, album, duration, duplicate Alpha entries, and lowercase eight-character CRC32.
- Controlled proof-gap RED: a temporary test passed `controlled-prefix-private-source-id-alpha-controlled-suffix` to the same helper. Exact command: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupIntegrationJvmTest.localOnlyCanaryAssertionRejectsAControlledLeak' --configuration-cache --rerun-tasks`. XML/Gradle recorded 1 test, 1 failure, 0 skipped; the assertion failed on the injected source-ID canary. The temporary leaking test was then removed.
- Unchanged-production GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlaylistBackupIntegrationJvmTest' --configuration-cache --rerun-tasks` passed; fresh XML records 1 test, 0 failures, 0 errors, 0 skipped. No production source changed and no production leak was exposed; this is evidence-strengthening, not a production RED/fix.

### Exact focused Task 1-8 JVM evidence

```bash
./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionStateTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' --tests 'com.eterocell.rhythhaus.BottomBarModeTest' --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --tests 'com.eterocell.rhythhaus.library.ui.TrackSelectionSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.library.ui.Task3ReviewSemanticsJvmTest' --tests 'com.eterocell.rhythhaus.search.SearchSelectionPoliciesJvmTest' --tests 'com.eterocell.rhythhaus.playlistbackup.*' --tests 'com.eterocell.rhythhaus.settings.SettingsScreenTest' --tests 'com.eterocell.rhythhaus.AppScanCancellationTest' --tests 'com.eterocell.rhythhaus.library.PlaylistRepositoryContractTest' --tests 'com.eterocell.rhythhaus.library.PlaylistSqlDelightRepositoryJvmTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 15s`; 35/35 Gradle tasks executed. Fresh `shared/build/test-results/jvmTest/TEST-*.xml` contains 20 selected suites and exactly 272 tests, 0 failures, 0 errors, 0 skipped. This includes the 1-test strengthened integration suite.

### Exact focused Android-host adapter evidence

```bash
./gradlew :shared:testAndroidHostTest --tests 'com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsAndroidTest' --configuration-cache --rerun-tasks
```

Result: `BUILD SUCCESSFUL in 7s`; 52/52 Gradle tasks executed. Fresh `shared/build/test-results/testAndroidHostTest/TEST-com.eterocell.rhythhaus.playlistbackup.PlatformPlaylistBackupDocumentsAndroidTest.xml` records exactly 10 tests, 0 failures, 0 errors, 0 skipped.

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

- Superseded focused summaries: the earlier Gradle-task-only counts are replaced by the exact commands and XML case counts above.
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
- The bounded-read adjudication passed for that specific defect. Whole-Task-9 final acceptance is pending the reopened privacy-proof and exact-evidence findings.

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

## Prior reviews and current OpenSpec status

- Controller-visible independent re-review at evidence HEAD `6af0ba85e629819937fcee60dad6d9909234ee45` returned Spec Compliance PASS and Task Quality PASS with no Critical, Important, or Minor findings. It confirmed all 80 canaries, retained positive portable metadata, exact JVM 272/0/0/0 and Android 10/0/0/0 XML counts, OpenSpec alignment, runtime gaps, and test/documentation-only scope.
- At committed evidence HEAD `6af0ba85e629819937fcee60dad6d9909234ee45`, `GIT_MASTER=1 git diff --check` exited 0 with no output; `git status --short` listed only the two excluded pre-existing generic Task 1/2 reports.
- OpenSpec 8.1, 8.2, 8.3, and 9.4 are complete. OpenSpec 9.1, 9.2, and 9.3 remain unchecked because runtime interaction, real system-panel presentation, and visual acceptance were unavailable.
- Immediate controller gate after the dedicated lifecycle-closure commit: run `GIT_MASTER=1 git diff --check`, `GIT_MASTER=1 git status --short`, and strict OpenSpec validation at the new HEAD. The resulting SHA and outputs are reported in the delivery response; no pre-commit final-HEAD pass is claimed here.

Next owner: user/manual QA or a future session with attachable desktop/mobile accessibility plus renderable screenshots. Archive only on an explicit later request.

Blockers: runtime interaction/system-panel and visual acceptance only. No privacy-proof, focused-count, independent-review, OpenSpec, or evidence-HEAD hygiene blocker remains.
