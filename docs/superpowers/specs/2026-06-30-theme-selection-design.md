# Theme Selection Design

## Context

RhythHaus currently uses one warm light palette through global color values in `HausColors.kt` and wraps the shared UI in `RhythHausTheme` with Miuix `lightColorScheme()`. Settings is already a shared Compose overlay, so the theme selector belongs there rather than in platform-specific settings screens.

The requested behavior is:
- app UI adapts to both light and dark themes;
- Settings exposes Dark, Light, and System choices;
- the choice persists across app restarts on Android, iOS, and macOS.

SQLite/SQLDelight is intentionally not used for this preference. Theme mode is app UI state, not library data. AndroidX DataStore Preferences is the selected persistence layer.

## Goals

- Add a shared app appearance model with `System`, `Light`, and `Dark` theme modes.
- Resolve `System` against platform/system dark mode where Compose exposes it.
- Persist the selected mode using DataStore Preferences across Android, iOS, and macOS/JVM.
- Update the shared Compose UI to draw from an active light/dark Haus palette instead of hardcoded light colors.
- Add an Appearance section to Settings with a clear selected state for System, Light, and Dark.
- Keep the existing RhythHaus visual identity: warm light theme, dark charcoal theme, and orange pulse accent.

## Non-goals

- No native platform settings screens.
- No SQLDelight schema/table for app preferences.
- No redesign of layout, typography, navigation, library scanning, or playback.
- No Windows/Linux-specific path support beyond the current macOS/JVM desktop target.
- No dynamic in-process OS theme-change listener beyond Compose/system-theme resolution available at composition time.

## Decisions

### Decision: DataStore Preferences for app settings

Use AndroidX DataStore Preferences as the app settings persistence mechanism. It is purpose-built for small preferences like a theme mode and avoids mixing UI preferences into the music-library database.

Use stable DataStore `1.2.1` unless implementation proves that the current KMP target layout requires a different stable-compatible artifact split. Avoid alpha dependencies unless there is no stable KMP-compatible option.

### Decision: Shared theme domain with platform path actuals

Common code owns:
- `enum class RhythHausThemeMode { System, Light, Dark }`
- stable serialized values: `system`, `light`, `dark`
- a parser that falls back to `System` for missing/invalid persisted values
- display labels/descriptions for Settings
- light and dark `HausColorPalette` values
- the DataStore-backed `ThemePreferenceStore` interface or implementation

Platform code supplies only the DataStore file path or factory details needed by Android, iOS, and JVM/macOS.

### Decision: CompositionLocal palette

Expose the active palette through a CompositionLocal, for example `LocalHausColors`. Composables should use the current palette instead of fixed globals. Existing names such as `HausInk` may remain only if they become compatibility accessors to `LocalHausColors.current` in composable code or if they are not used by themed UI.

The theme wrapper should choose `lightColorScheme()` or `darkColorScheme()` for Miuix and provide corresponding Miuix slots:
- `primary`/`onPrimary`
- `secondary`/`onSecondary`
- `background`/`onBackground`
- `surface`/`onSurface`
- `surfaceContainer`/`onSurfaceContainer`
- secondary variant and disabled colors

### Decision: Settings owns selection UI, App owns state

`App()` creates the store, observes the selected mode, and provides callbacks. `SettingsScreen` receives:
- current `RhythHausThemeMode`
- `onThemeModeSelected: (RhythHausThemeMode) -> Unit`

When the user taps a mode, the UI updates and the mode is persisted. Failed persistence should not crash the UI; the app may keep the in-memory selection and optionally surface an import/settings message later, but this slice does not add a new error banner.

## Risks / Trade-offs

- DataStore adds new dependencies and a coroutine/Flow API. The app already uses coroutines, and this is preferable to SQLite for preferences.
- Migrating all visible color call sites in one slice touches large shared Compose files. Keep the change mechanical and avoid unrelated UI refactoring.
- Dark theme quality is only compile/test verified automatically. Manual visual validation on Android/iOS/macOS remains recommended.

## Verification

- Common tests for theme-mode serialization/parsing, resolution, and settings option order/labels.
- JVM test for DataStore-backed persistence if practical without platform UI.
- `openspec validate theme-selection --strict`.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- `/usr/bin/xcrun xcodebuild -version` and `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` if iOS actuals are touched.
