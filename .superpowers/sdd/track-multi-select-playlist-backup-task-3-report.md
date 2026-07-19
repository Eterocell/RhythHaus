# Task 3 Report: Contextual Bottom Bar and Multi-Track Picker

This change-specific report preserves the Task 3 evidence originally committed at the generic path in `71888de`. The generic `.superpowers/sdd/task-3-report.md` is restored byte-for-byte from base `64112b6`; all subsequent Task 3 evidence updates belong here.

## Original Task 3 delivery

- Route: `openspec+superpowers` / approved Task 3 execution.
- Commit: `71888de` (`feat: add contextual selection playlist flow`).
- `LibraryAppShell` owns one remembered `TrackSelectionState`; Back, route changes, stale-detail recovery, and Home browse-mode changes follow the approved selection lifetime.
- Picker input is derived only through `orderedSelectedTrackIds` from the active page's visible order. Picker state and mutation requests retain ordered `List<String>` values; exact constructor-invariant coverage was added in the follow-up below.
- Picker dismissal and mutation failure retain selection; successful append/inline-create close the picker and clear selection.
- One measured bottom slot renders Selection before Now Playing, with localized English/Chinese count, cancel, and add semantics.
- Original focused Task 1-3 tests passed. A daemon-stopped sequential broad gate passed 325 JVM tests, desktop compilation, and Android debug assembly.
- Original source reviews passed after matching measurements were made authoritative for clearance, offset, and alpha.
- Runtime visual/touch QA was not performed and was not claimed.

## Independent-review repair

### Findings verified

1. `HomeSongs` selection precedence covered album, artist, playlist-detail, and Home routes, but omitted `PlaylistHub` even though the wide Home master remains visible and interactive there.
2. A new/stale keyed bar was composed at alpha zero before its matching measurement arrived, leaving child actions available because alpha does not suppress pointer input or semantics.
3. Picker rendering was nested under `libraryTracks.firstOrNull { it.id == picker.trackIds.first() }`, so loss of the first selected model hid the list-oriented picker despite retained later IDs.
4. `71888de` replaced the pre-existing generic Task 3 report instead of writing change-specific evidence.

### Strict RED

Focused command:

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.BottomBarModeTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.Task3ReviewSemanticsJvmTest' \
  --configuration-cache
```

The first clean RED failed compilation only on the wished-for contracts:

- missing `TrackSelectionBar(interactive = ...)`;
- missing `libraryBottomBarPresentation(...)` / `isInteractive` policy;
- missing list-only `AddToPlaylistPicker` signature (the old API required a resolved `track`).

After those contracts compiled, the PlaylistHub regression exercised the missing visible-master route policy. The real Compose tests also exposed the actual semantics structure: unmeasured/stale bars initially retained action nodes, and picker action matching had to remain locale-independent because the test environment rendered Chinese resources.

### Minimal production fixes

- Added `LibraryRoute.PlaylistHub` to the routes where a visible interactive Home master permits `HomeSongs` selection precedence.
- Added `LibraryBottomBarPresentation(clearancePx, alpha, isInteractive)`, derived from the exact current content measurement. Null/stale measurements produce zero clearance, zero alpha, and `isInteractive = false`.
- Passed `isInteractive` into `TrackSelectionBar`; non-interactive bars render selected-count presentation only and compose no Cancel/Add controls, so they expose no pointer target or `OnClick` semantics. Alpha is presentation only, not the suppression mechanism.
- Kept the existing Miuix controls and 44dp action sizing for the measured active bar.
- Removed `LibraryTrack` from `AddToPlaylistPicker`; `LibraryAppShell` now renders picker state directly without resolving `trackIds.first()`.

### GREEN

The focused command passed 12 tests (`BUILD SUCCESSFUL in 2s`), including:

- Home selection precedence was covered for wide detail content, but the exact `PlaylistHub` + Now Playing-visible production-policy case was not committed until the follow-up below;
- null/stale measurements are explicitly non-interactive;
- unmeasured and stale selection bars expose zero `OnClick` action nodes and dispatch no callbacks;
- matching measured selection bar exposes exactly two actions and dispatches Cancel/Add exactly once;
- picker remains visible and dismissible for `trackIds = ["missing", "track-b"]`;
- picker append remains usable and emits the retained ordered IDs `["missing", "track-b"]`.

### Evidence repair

- Base generic report source: `64112b6:.superpowers/sdd/task-3-report.md`.
- Historical generic SHA-256: `49ffed72694ac431cf419987e7cb9f80a14ec45c2f4a6a23032bd53d1ccdb521`.
- The generic report is restored from that exact Git object; future updates are confined to this change-specific report.

### Diagnostics and remaining verification

- Kotlin LSP is unavailable because `kotlin-ls` is not installed and installation was previously declined; Gradle Kotlin compilation is the executable diagnostic.
- Final focused Task 1-3 command included `TrackSelectionStateTest`, `PlaylistStateTest`, `PlaylistScreensTest`, `BottomBarModeTest`, `LibraryNavigationTest`, `TrackSelectionSemanticsJvmTest`, `Task3ReviewSemanticsJvmTest`, and `SearchSelectionPoliciesJvmTest` with `--rerun-tasks`. Result: `BUILD SUCCESSFUL in 13s`; 35/35 tasks executed.
- The single sequential broad command was `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`. Result: `BUILD SUCCESSFUL in 7s`; 101 actionable tasks (12 executed, 89 up-to-date). This covers the full JVM suite, desktop compilation, and Android debug assembly.
- The unchanged Android `MediaMetadata.Builder.setArtworkData` deprecation warning remains; no new warning or failure was introduced.
- `GIT_MASTER=1 git diff --check`: pass.

## Remaining re-review follow-up

### Prior evidence correction

The first repair report overstated two committed-test details:

- it described the exact `HomeSongs` + wide `PlaylistHub` + Now Playing-visible policy regression, but the committed pure test covered a generic wide detail route rather than that exact combination;
- it described ordered non-empty picker/request lists without committed constructor tests for every list-valued state/request, and append/inline-create requests did not yet enforce the invariant themselves.

This follow-up adds the exact tests and production guards. No claim is made for tests not present in the committed follow-up diff.

### Root cause

`LibraryBottomBarPresentation.isInteractive` was authoritative only for `TrackSelectionBar`. `LibraryAppShell` did not pass it to `NowPlayingBar`, whose root expand click, vertical sheet gesture, play/pause, Search, and Settings pointer modifiers remained active while a null/stale measurement forced alpha to zero.

### Clean RED

```text
./gradlew :shared:jvmTest \
  --tests 'com.eterocell.rhythhaus.BottomBarModeTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistStateTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.PlaylistScreensTest' \
  --tests 'com.eterocell.rhythhaus.library.ui.Task3ReviewSemanticsJvmTest' \
  --configuration-cache --rerun-tasks
```

Result: `BUILD FAILED in 9s`; test compilation failed only at the two production `NowPlayingBar` calls because no `interactive` parameter existed. This isolated the missing production interaction contract before implementation.

### Minimal fix and focused GREEN

- `NowPlayingBar(interactive)` conditionally installs all four click modifiers and includes `interactive` in the existing vertical-sheet `isActive` gate.
- `LibraryAppShell` passes the same `bottomBarPresentation.isInteractive` used by Selection, so null/stale measurements—not alpha—suppress interaction for both variants.
- Real Compose tests cover null/unmeasured zero actions, stale/mismatched zero actions plus physical play/search/settings/root taps and root swipe dispatching no callbacks, and matching measurement restoring exactly four production click actions and callbacks.
- Added the exact `HomeSongs` + `PlaylistHub` + Now Playing-visible policy regression.
- Added empty and embedded-blank constructor tests for `PlaylistPickerState`, `AddToPlaylistPickerState`, `PlaylistAppendRequest`, and `PlaylistInlineCreateRequest`; the request constructors now enforce the same non-empty/non-blank invariant.

Focused GREEN for the four follow-up classes: `BUILD SUCCESSFUL in 7s`; 26 actionable tasks (8 executed, 18 up-to-date). After strengthening inactive mode to omit the entire gesture modifier and making pointer tests locale-independent, the real Compose follow-up class passed again in 2s.

### Final verification

- Complete Task 1-3 focused matrix with `--rerun-tasks`: `BUILD SUCCESSFUL in 8s`; 26/26 tasks executed.
- Sequential `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: `BUILD SUCCESSFUL in 4s`; 101 actionable tasks (12 executed, 89 up-to-date).
- The unchanged Android `MediaMetadata.Builder.setArtworkData` deprecation warning remains.
- Kotlin LSP remains unavailable because `kotlin-ls` is not installed and installation was previously declined; both JVM and Android Kotlin compilation passed.
