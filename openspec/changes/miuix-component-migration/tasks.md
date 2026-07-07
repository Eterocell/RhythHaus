# Tasks

- [x] 1. Add and verify needed Miuix component dependencies.
  - [x] Added `miuix-preference` alias using `version.ref = "miuix"` for the planned Settings dropdown/preference component.
  - [x] Added no other Miuix aliases.
  - [x] Did not add `miuix-navigation3-adaptive`.
  - [x] Shared JVM compile passed after dependency changes: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> `BUILD SUCCESSFUL in 27s`.
  - [x] Android debug assembly passed after dependency changes: `./gradlew :androidApp:assembleDebug --configuration-cache` -> `BUILD SUCCESSFUL in 45s`; `:androidApp:checkDebugDuplicateClasses` and manifest processing completed without duplicate-class/manifest failure.

- [x] 2. Replace Settings appearance dropdown with a Miuix component.
  - [x] Replaced custom `AppearanceDropdown` / `AppearanceDropdownOption` with Miuix `OverlayDropdownPreference`.
  - [x] Preserved System/Light/Dark labels via `RhythHausThemeMode.settingsOptions.map { it.displayLabelResource() }`, current-mode summary via `displayDescriptionResource()`, selected state via `selectedIndex`, and callback behavior via `options.getOrNull(index)?.let(onThemeModeSelected)`.
  - [x] Added Miuix `Scaffold` popup host around the Settings content with `containerColor = HausColors.current.paper` and `contentWindowInsets = WindowInsets(0.dp)`; dropdown uses `renderInRootScaffold = false` inside that host.
  - [x] Removed the duplicate standalone `Appearance` section header above `OverlayDropdownPreference`; only the Miuix preference title remains.
  - [x] Shared JVM compile passed after duplicate-label fix: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> `BUILD SUCCESSFUL in 2s`; `16 actionable tasks: 4 executed, 12 up-to-date`; configuration cache reused.
  - [x] Android debug assembly passed after duplicate-label fix: `./gradlew :androidApp:assembleDebug --configuration-cache` -> `BUILD SUCCESSFUL in 3s`; `:androidApp:checkDebugDuplicateClasses` was up-to-date and no duplicate-class/manifest failure occurred.
  - [x] Diff hygiene passed: `git diff --check` -> no output, exit 0.
  - [x] Scoped Task 2 diff-name check excludes the pre-existing protected iOS scheme: `git diff --name-only -- shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt openspec/changes/miuix-component-migration/tasks.md` -> `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt` before this evidence update; no protected-file content was read or touched.

- [x] 3. Selectively migrate Search standard UI pieces.
  - [x] Audited current `SearchScreen.kt` and Miuix 0.9.3 `TextField` / `IconButton` source signatures from the cached `miuix-ui-0.9.3-sources.jar`.
  - [x] Replaced the hand-rolled search-field `Box` + `BasicTextField` + custom clear chip with Miuix `TextField` and `IconButton`, preserving `FocusRequester`, `query` state updates/filtering, placeholder string, single-line input, clear action, and pulse cursor/text styling.
  - [x] Wrapped the Miuix `TextField` in a stable always-visible `1.dp` muted `RoundedCornerShape(12.dp)` border container after review found Miuix only draws its own `2.dp` border while focused; the Miuix focused border color is transparent to avoid double/thick border while preserving the old unfocused readability affordance.
  - [x] Kept `SearchResultRow` custom apart from its existing Miuix `Surface`; `BasicComponent` was not forced because the current row cleanly preserves now-playing background, title pulse color, equalizer display, and click-to-queue/play/dismiss behavior.
  - [x] Border-parity fix shared JVM compile passed: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> `BUILD SUCCESSFUL in 3s`; `16 actionable tasks: 11 executed, 5 up-to-date`; configuration cache reused.
  - [x] Border-parity fix corrected relevant test passed: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` -> `BUILD SUCCESSFUL in 776ms`; `25 actionable tasks: 7 executed, 18 up-to-date`; configuration cache reused.
  - [x] Border-parity fix diff hygiene passed: `git diff --check` -> no output, exit 0.
  - [x] Requested test command exposed a stale package filter: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.LibraryNavigationTest' --configuration-cache` -> failed with `No tests found for given includes`; the test class is in package `com.eterocell.rhythhaus.library.ui`.
  - [x] Corrected relevant test passed: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` -> `BUILD SUCCESSFUL in 1s`; `34 actionable tasks: 5 executed, 29 up-to-date`.

- [x] 4. Selectively migrate Library rows and clear-library dialog pieces.
  - [x] Audited `LibraryRows.kt` `TrackRow`, `ArtistRow`, and `AlbumCard` against Miuix 0.9.3 `BasicComponent` from the cached `miuix-ui-0.9.3-sources.jar`.
  - [x] Kept `TrackRow` custom: `BasicComponent` is available, but it would replace the current 14dp artwork/title/duration row spacing with its fixed 8dp start/end layout, introduce Miuix's own clickable modifier instead of `hausClickable`, and make selected border/background + trailing duration semantics less direct. Existing artwork mark, selected badge, content description, click behavior, duration display, and spacing are preserved by no source change.
  - [x] Kept `ArtistRow` custom for the same row-layout reasons, preserving circular artist artwork/initial fallback, content description, click behavior, album/track metadata display, and spacing.
  - [x] Kept `AlbumCard` artwork/gradient placeholder custom as product-specific music UI; existing Miuix `Card` remains the standard container.
  - [x] Audited `AnimatedClearLibraryDialogRoute`; kept the full-screen route scrim and tap-to-dismiss shell so route transition participation remains intact. Inner standard pieces were already Miuix `Card`, `Button`, and `Text`, so no `OverlayDialog` migration was forced.
  - [x] Focused shared JVM compile passed: `./gradlew :shared:compileKotlinJvm --configuration-cache` -> `BUILD SUCCESSFUL in 366ms`; `16 actionable tasks: 3 executed, 13 up-to-date`; configuration cache reused.
  - [x] Corrected relevant navigation test passed: `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` -> `BUILD SUCCESSFUL in 326ms`; `25 actionable tasks: 4 executed, 21 up-to-date`; configuration cache reused.
  - [x] Diff hygiene passed: `git diff --check` -> no output, exit 0.
  - [x] Scoped Task 4 diff-name check showed no source-code edits to `LibraryRows.kt` or `LibraryDialogs.kt`; only this evidence/report work is in scope for Task 4. Pre-existing protected `iosApp/iosApp.xcodeproj/xcshareddata/xcschemes/iosApp.xcscheme` remained untouched.

- [x] 5. Final verification and handoff evidence.
  - [x] OpenSpec validation passed: `openspec validate miuix-component-migration --strict` -> `Change 'miuix-component-migration' is valid`.
  - [x] Broad JVM/desktop/Android verification initially exposed a transient pre-existing playback test failure: `JvmPlaybackEngineTest.controllerAutoAdvancesToNextTrackOnCompletion` failed once in `:shared:jvmTest`; targeted rerun passed with `BUILD SUCCESSFUL in 1s`.
  - [x] Broad JVM/desktop/Android verification passed on rerun: `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache` -> `BUILD SUCCESSFUL in 2s`; `99 actionable tasks: 7 executed, 92 up-to-date`; configuration cache reused.
  - [x] Xcode is available: `/usr/bin/xcrun xcodebuild -version` -> `Xcode 26.6`, `Build version 17F113`.
  - [x] iOS simulator verification passed: `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` -> `BUILD SUCCESSFUL in 18s`; `43 actionable tasks: 23 executed, 20 up-to-date`; configuration cache stored.
  - [x] Diff hygiene passed: `git diff --check` -> no output, exit 0.
  - [x] Updated `progress.md` and `roadmap.md` with migration scope, verification evidence, pre-existing protected iOS scheme exclusion, and manual visual QA next owner.
