# Package Organization Design

## Overview

RhythHaus just split the shared Compose library UI into focused files. The next architecture step is to move those files into focused Kotlin packages so file location matches responsibility. This is a behavior-preserving package refactor: source files move, package declarations/imports update, and tests move only where package-private access requires it.

## Goals

- Make shared Compose/UI package boundaries match feature ownership and reusable helper ownership.
- Keep `App()` as the root shared entry point at `com.eterocell.rhythhaus.App`.
- Preserve existing `library` scanner/repository/source-access/persistence package boundaries.
- Preserve existing `taglib` module/package boundaries.
- Preserve route behavior, adaptive behavior, playback behavior, scanner/import behavior, Miuix/blur behavior, strings, content descriptions, and platform seams.
- Avoid dependency/toolchain/resource/product changes.

## Non-goals

- No visual redesign.
- No screen behavior changes.
- No scanner, repository, database, playback engine, TagLib, source-access, artwork-cache, or theme behavior rewrite.
- No native navigation migration.
- No Swift-facing API renames unless explicitly needed to keep `App()` working.
- No Windows/Linux scope.

## Package design

Use a hybrid feature-first organization:

```text
com.eterocell.rhythhaus
  App.kt                         # root shared entry point only
  library/                       # existing scanner/repository/source-access/persistence
  library/ui/                    # library feature UI, shell, routes, coordinator, navigation decisions
  nowplaying/                    # Now Playing screen and bottom bar UI
  search/                        # Search overlay/screen UI
  settings/                      # Settings overlay/screen UI
  ui/                            # shared Compose UI helpers and gestures
  theme/                         # Haus palette/theme and theme preference store expect/actual
  playback/                      # playback controller/engine/dispatchers/service/transport bridge
  model/                         # cross-feature app/domain models and metadata helpers if moved
```

The refactor should proceed in stages and stop before any package move that creates Swift/framework churn or expect/actual mismatch risk that is disproportionate to the organization benefit.

## Package responsibilities

### Root package

`com.eterocell.rhythhaus` remains the stable app entry namespace. Keep `App()` available at the current package and do not require Android/iOS/desktop app entry points to learn new app-entry imports unless the implementation plan explicitly includes those import updates.

### `library`

Keep existing non-UI local-library infrastructure under `com.eterocell.rhythhaus.library`, including scanner, repository, source access, database, path resolving, and library utility seams.

### `library.ui`

Move the library feature UI/orchestration files here:

- `LibraryAppShell.kt`
- `LibraryAppState.kt`
- `LibraryRoutes.kt`
- `LibraryHomeContent.kt`
- `LibraryDetailContent.kt`
- `LibraryChrome.kt`
- `LibraryDialogs.kt`
- `LibraryRows.kt`
- `LibraryNavigation.kt`
- `LibraryBrowser.kt`, if the browse grouping types are only consumed by the library UI layer

This package may import model/playback/theme/shared UI helpers, and it may import `com.eterocell.rhythhaus.library` data/source types. It should not own scanner/repository/database implementation.

### `nowplaying`

Move Now Playing screen/bar UI here:

- `NowPlayingScreen.kt`
- `NowPlayingBar.kt`

Keep platform media-session/lockscreen/engine implementation out of this package unless it is purely UI. `NowPlayingArtworkBridge.kt` is platform media bridge code and should stay with playback/platform code unless a later spec moves it.

### `search` and `settings`

Move overlay screen UI files here:

- `SearchScreen.kt` -> `com.eterocell.rhythhaus.search`
- `SettingsScreen.kt` -> `com.eterocell.rhythhaus.settings`

These packages may import library data models and playback abstractions, but should not own scanner/repository/database implementations.

### `ui`

Move reusable Compose helpers here only when they are genuinely shared across features:

- `BackChip.kt`
- `HausClickable.kt`
- `SwipeBackGesture.kt`
- `VerticalSheetGesture.kt`
- `MusicProgressScrubber.kt`
- `LiquidGlassChrome.kt`
- `ArtworkDecoder.kt` expect/actual files, if package alignment is kept across all source sets

### `theme`

Move theme files here with expect/actual package alignment:

- `HausColors.kt`
- `Theme.kt`
- `ThemePreferenceStore.kt` and platform actuals

The public theme behavior and selected-theme persistence must not change.

### `playback` and `model`

Move playback/domain files only after lower-risk UI package moves compile:

- `Playback.kt`, `PlaybackEngine.*`, `PlaybackDispatchers.*`, `RhythHausPlaybackService.kt`, `RhythHausTransportBridge.kt` -> `playback`
- `MusicModels.kt`, `AudioMetadata.*`, `ImportLabels.*` -> `model` if package alignment is safe

If moving these would force broad platform bridge churn or Swift API churn, leave them in root for this change and document the deferral.

## Testing and verification

Intermediate tasks should run focused compilation and tests relevant to moved files. Final verification must run:

- `openspec validate package-organization --strict`
- `./gradlew :shared:jvmTest --tests 'com.eterocell.rhythhaus.library.ui.LibraryNavigationTest' --configuration-cache` or the final package-correct focused test name
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`
- `/usr/bin/xcrun xcodebuild -version`
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache`
- `git diff --check`

## Risks

- Kotlin package-private `internal` access may hide missing imports until compilation; move in small batches.
- Expect/actual package declarations must match exactly; theme/artwork/playback/model moves should include every platform source-set actual in the same task.
- Tests in the old root package may lose access to package-private helpers after `LibraryNavigation.kt` moves; move or import tests intentionally.
- iOS/Swift entry points may rely on exported symbol names. Preserve `App()` at the root package unless a task explicitly updates all platform entry points and verifies iOS.
