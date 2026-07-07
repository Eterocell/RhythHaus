# Miuix TopAppBar Migration Tasks

- [x] 1. Create shared RhythHaus Miuix top app bar wrapper.
  - Add `RhythHausTopAppBar` under shared UI using Miuix `SmallTopAppBar` and Miuix `IconButton`.
  - Use existing Haus colors and disable default window inset padding because callers own safe/status padding.
  - Provide optional back navigation with localized back content description.
  - Verified `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 3s`; 16 actionable tasks: 4 executed, 12 up-to-date; configuration cache entry reused).

- [x] 2. Replace Search and Settings custom top bars.
  - Replaced the `BackChip` + title `Row` in `SearchScreen.kt` with `RhythHausTopAppBar(title = stringResource(Res.string.search), onBack = onDismiss)`.
  - Replaced the `BackChip` + title `Row` in `SettingsScreen.kt` with `RhythHausTopAppBar(title = stringResource(Res.string.settings), onBack = onDismiss)`.
  - Preserved Search focus requester, query state, placeholder, clear action, filtering, result selection, now-playing highlight, equalizer, and dismiss behavior; only the top chrome block/imports changed.
  - Preserved Settings Miuix `Scaffold`, appearance dropdown, scanning card, folder picker, import message, and clear-library behavior; only the top chrome block/imports changed.
  - Verified `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`; 16 actionable tasks: 4 executed, 12 up-to-date; configuration cache entry reused).
  - Verified `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 964ms`; 25 actionable tasks: 7 executed, 18 up-to-date; configuration cache entry reused).

- [x] 3. Replace Library drill-down header back/subtitle row.
  - Replaced only the `BackChip` + subtitle row in `DrillDownHeader` with `RhythHausTopAppBar(title = subtitle, onBack = onBack)`.
  - Kept the large drill-down title below the top app bar.
  - Preserved `DrillDownView`, `NestedScrollBlurChrome`, `TrackRow`, `SectionLabel`, list content padding, scroll reporting, left-edge swipe back, and Now Playing bar behavior; only `LibraryRows.kt` header chrome/imports changed.
  - Verified `./gradlew :shared:compileKotlinJvm --configuration-cache`: pass (`BUILD SUCCESSFUL in 2s`; 16 actionable tasks: 4 executed, 12 up-to-date; configuration cache entry reused).
  - Verified `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`: pass (`BUILD SUCCESSFUL in 723ms`; 25 actionable tasks: 6 executed, 19 up-to-date; configuration cache entry reused).
  - Verified `openspec validate miuix-top-app-bar --strict`: pass (`Change 'miuix-top-app-bar' is valid`).

- [x] 4. Final verification and handoff evidence.
  - Verified `openspec validate miuix-top-app-bar --strict`: pass (`Change 'miuix-top-app-bar' is valid`).
  - Verified `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 14 executed, 85 up-to-date; configuration cache entry reused). Existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`.
  - Verified `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
  - Verified `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 12s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache entry reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest`.
  - Verified `git diff --check`: pass (no output, exit 0).
  - Updated `progress.md` and `roadmap.md` with handoff evidence and manual QA next action.
