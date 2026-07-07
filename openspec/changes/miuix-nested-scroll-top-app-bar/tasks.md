# Miuix Nested-Scroll TopAppBar Tasks

- [x] 1. Extend `RhythHausTopAppBar` for glass-backed nested-scroll usage.
  - Preserve the existing required API shape: `RhythHausTopAppBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, subtitle: String = "")`.
  - Add only optional parameters after existing parameters, if needed, for background/title/subtitle colors, inset behavior, and title padding.
  - Keep existing defaults equivalent for Search, Settings, and `DrillDownHeader`.
  - Verify `./gradlew :shared:compileKotlinJvm --configuration-cache`.
  - Evidence (2026-07-07): `./gradlew :shared:compileKotlinJvm --configuration-cache` → BUILD SUCCESSFUL in 9s; 16 actionable tasks: 4 executed, 12 up-to-date; Configuration cache entry reused.

- [x] 2. Migrate `NestedScrollBlurChrome` toolbar content to Miuix top app bar.
  - Replace the custom collapsed title `Row`/pulse-dot/`Text` in `LibraryChrome.kt` with a Miuix top app bar rendering path.
  - Prefer `RhythHausTopAppBar(title = title, onBack = null, ...)` using transparent/glass-compatible color and caller-owned insets.
  - Preserve the outer glass overlay, `chromeHeight`, early return at `progress <= 0f`, `titleProgress` alpha threshold, and bottom divider.
  - Do not change `LibraryHomeContent.kt`, `LibraryDetailContent.kt`, `nestedScrollChromeStateFor(...)`, bottom-bar visibility logic, route transitions, or platform files unless compile proves a minimal import fix is necessary.
  - Verify `./gradlew :shared:compileKotlinJvm --configuration-cache`.
  - Evidence (2026-07-07): `./gradlew :shared:compileKotlinJvm --configuration-cache` → BUILD SUCCESSFUL in 4s; 16 actionable tasks: 4 executed, 12 up-to-date; Configuration cache entry reused.
  - Verify `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache`.
  - Evidence (2026-07-07): `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` → BUILD SUCCESSFUL in 1s; 25 actionable tasks: 7 executed, 18 up-to-date; Configuration cache entry reused.
  - Verify `openspec validate miuix-nested-scroll-top-app-bar --strict`.
  - Evidence (2026-07-07): `openspec validate miuix-nested-scroll-top-app-bar --strict` → `Change 'miuix-nested-scroll-top-app-bar' is valid`.
  - Verify `git diff --check`.
  - Evidence (2026-07-07): `git diff --check` → pass (no output, exit 0).

- [x] 3. Final verification and handoff evidence.
  - Verified `openspec validate miuix-nested-scroll-top-app-bar --strict`: pass (`Change 'miuix-nested-scroll-top-app-bar' is valid`).
  - Verified `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`: pass (`BUILD SUCCESSFUL in 5s`; 99 actionable tasks: 13 executed, 5 from cache, 81 up-to-date; configuration cache entry reused). Existing Android deprecation warning only: `MediaMetadata.Builder.setArtworkData`.
  - Verified `/usr/bin/xcrun xcodebuild -version`: pass (`Xcode 26.6`, `Build version 17F113`).
  - Verified `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`: pass (`BUILD SUCCESSFUL in 11s`; 34 actionable tasks: 8 executed, 26 up-to-date; configuration cache entry reused). Existing iOS test warnings only in `IOSNowPlayingBridgingTest`.
  - Verified `git diff --check`: pass (no output, exit 0).
  - Updated `progress.md` and `roadmap.md` with handoff evidence and manual QA next action.
