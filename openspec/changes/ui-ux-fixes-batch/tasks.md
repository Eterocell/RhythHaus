# Tasks

## 1. Empty Home onboarding and adaptive album grid

- [x] Add common tests for album-grid column breakpoints.
  - Evidence: added `LibraryBrowserTest` covering 0/559dp => 2 columns, 560/899dp => 3 columns, and 900/1400dp => 4 columns.
- [x] Add the pure column-count helper in shared common code.
  - Evidence: added `albumGridColumnsForWidth(widthDp: Float): Int` in `LibraryBrowser.kt`.
- [x] Show `ImportAudioCard` on Home when the library is empty.
  - Evidence: `LibraryHomeScreen` now inserts `ImportAudioCard` after `HeaderSection(snapshot)` when `snapshot.tracks.isEmpty()`.
- [x] Render album rows using the adaptive column count instead of hardcoded two-column chunks.
  - Evidence: album mode now wraps rendering in `BoxWithConstraints`, computes columns from `maxWidth.value`, chunks albums by that count, and preserves spacer fill for short rows.
- [x] Run focused common tests and JVM compile.
  - RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache` failed before implementation with unresolved reference `albumGridColumnsForWidth`.
  - GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache` passed (`BUILD SUCCESSFUL`).
  - GREEN: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL`).

## 2. Songs browse mode

- [x] Extend `BrowseMode` with `Songs`.
  - Evidence: `BrowseMode` is now `Albums, Artists, Songs`; added `LibraryBrowserTest.browseModesIncludeAlbumsArtistsAndSongsInOrder`.
- [x] Render all tracks as `TrackRow` entries in Songs mode.
  - Evidence: Home browse rendering now uses a `when (browseMode)` and the `BrowseMode.Songs` branch renders `items(snapshot.tracks, key = { it.id })` with `TrackRow`.
- [x] Wire song-row clicks to select the track and use the full-library playable queue to start/toggle playback.
  - Evidence: song row clicks set `selectedTrackId`, build `snapshot.tracks.map { it.toPlayableTrack() }`, set the queue when needed, and call `playbackController.togglePlayPause()`.
- [x] Preserve Albums and Artists behavior.
  - Evidence: existing Albums adaptive-grid and Artists drill-down rendering were preserved in the new `when` branches.
- [x] Run focused common tests and JVM compile.
  - RED: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache` failed before implementation with unresolved reference `BrowseMode.Songs`.
  - GREEN: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryBrowserTest' --configuration-cache` passed (`BUILD SUCCESSFUL`).
  - GREEN: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL`).

## 3. Search and compact controls polish

- [x] Add Search query clear action.
  - Evidence: `SearchScreen` now reserves 44dp trailing field space when the query is non-empty and shows an end-aligned `Clear` action that uses Miuix `Text` plus `Modifier.hausClickable` to empty the query.
- [x] Dismiss Search after a result starts playback.
  - Evidence: `SearchResultRow` selection still sets the full library queue and calls `playbackController.play()`, then calls `onDismiss()`.
- [x] Increase `BackChip` effective hit target to at least 44dp height.
  - Evidence: `BackChip` outer modifier now applies `heightIn(min = 44.dp)` before inner padding.
- [x] Increase bottom-bar Search and Settings effective hit targets to at least 44dp.
  - Evidence: `NowPlayingBar` Search and Settings boxes now use `.size(44.dp)` while preserving 18dp icon size, content descriptions, and row arrangement.
- [x] Run JVM compile.
  - GREEN: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL`).

## 4. Remove user-facing developer panels

- [ ] Remove normal UI rendering of TagLib developer panels.
- [ ] Remove now-unused developer-only composables/imports when they become dead code.
- [ ] Verify source no longer contains user-facing `DEV · TagLib` text.
- [ ] Run broad JVM/desktop/Android verification.

## 5. Handoff

- [ ] Run `openspec validate ui-ux-fixes-batch --strict`.
- [ ] Run final relevant verification or record exact blockers.
- [ ] Update this task list with evidence.
- [ ] Update `progress.md` with route, scope, verification, changed files, next owner, blockers, and commits.
- [ ] Commit with a semantic message after successful OpenSpec + Superpowers execution.
