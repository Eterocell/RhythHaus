# Drill-Down Top-Bar Artwork Tasks

- [x] 1. Pass representative artwork candidates into drill-down chrome.
  - Added optional `topBarArtworkCandidates: List<ByteArray> = emptyList()` to `DrillDownView`.
  - Derived album artwork candidates in `LibraryRouteContent` from `albumTracks.mapNotNull { it.artworkBytes }`.
  - Derived artist artwork candidates in `LibraryRouteContent` from `artistTracks.mapNotNull { it.artworkBytes }`.
  - Forwarded the candidates from `DrillDownView` into `DrillDownMiuixScrollChrome`.
  - Evidence: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed after review fix (`BUILD SUCCESSFUL in 2s`; 16 actionable tasks: 4 executed, 12 up-to-date; configuration cache reused).

- [x] 2. Render cached artwork as the Miuix drill-down top-bar background.
  - Added optional `topBarArtworkCandidates: List<ByteArray> = emptyList()` to `DrillDownMiuixScrollChrome`.
  - Decoded with `remember(topBarArtworkCandidates) { topBarArtworkCandidates.firstNotNullOfOrNull { it.decodeArtworkCached() } }`, so later candidates are tried if earlier artwork fails to decode.
  - Rendered non-null artwork as a full-width square top bar at rest that height-animates/compacts into a rectangular top bar while scrolling image with a readability scrim, circular back-button background, and chip-style expanded/collapsed title overlays.
  - Rendered no placeholder when artwork is absent or all candidates fail to decode; the existing glass title top bar remains in that fallback path.
  - Evidence: `./gradlew :shared:compileKotlinJvm --configuration-cache` passed (`BUILD SUCCESSFUL in 2s`; 16 actionable tasks: 4 executed, 12 up-to-date; configuration cache reused).
  - Evidence: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryBrowserTest' --configuration-cache` passed (`BUILD SUCCESSFUL in 868ms`; 25 actionable tasks: 7 executed, 18 up-to-date; configuration cache reused).

- [x] 3. Final verification and handoff evidence.
  - Evidence: `openspec validate drilldown-topbar-artwork --strict` passed (`Change 'drilldown-topbar-artwork' is valid`).
  - Evidence: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` passed (`BUILD SUCCESSFUL in 3s`; 99 actionable tasks: 12 executed, 87 up-to-date; existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`).
  - Evidence: `/usr/bin/xcrun xcodebuild -version` passed (`Xcode 26.6`, `Build version 17F113`).
  - Evidence: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` passed (`BUILD SUCCESSFUL in 9s`; 34 actionable tasks: 8 executed, 26 up-to-date; existing iOS test warnings only in `IOSNowPlayingBridgingTest`).
  - Evidence: `git diff --check` passed (no output, exit 0).
  - Updated this file with evidence.
  - Updated `progress.md` and `roadmap.md`.
