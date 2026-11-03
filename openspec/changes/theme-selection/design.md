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

## Approved extension — selectable palettes and Android Monet

This extension adds the theme-selection surface required by the approved platform-adaptive redesign. It supersedes only the earlier single light/dark palette and orange-accent limit, the Appearance dropdown presentation, the no-dynamic-color limitation, and the no-Settings-layout-redesign limit. DataStore remains the preference owner; SQLDelight, playback, scanning, library navigation, and native Settings screens remain out of scope. Theme selection stays separate from the `platform-adaptive-ui-redesign` change.

### Appearance model and resolution

- Keep `RhythHausThemeMode` (`System`, `Light`, `Dark`) as an independent persisted preference; its default remains `System`.
- Add stable palette IDs `moss`, `bay`, `clay`, and `iris`; default to `moss`. Every curated palette has a complete Light and Dark value set.
- Persist the selected curated palette independently from theme mode and Android dynamic-color intent. Invalid/missing palette IDs resolve to Moss; invalid/missing mode remains System.
- Android Monet is opt-in and available only on Android API 31+. Resolve wallpaper colors through an Android-owned provider; common UI and feature modules receive only immutable semantic palette data, never `Context` or Android color APIs. iOS, macOS/JVM, and API 29–30 resolve the selected curated palette.
- The effective Android palette uses the system's current light/dark dynamic scheme when Monet is enabled. Re-resolve when the active Android configuration/system appearance changes. If dynamic colors cannot be supplied, render the selected curated palette, expose a recoverable unavailable state on supported Android, and retain the user's Monet intent.
- Selecting a curated palette disables Monet. Turning Monet off restores the previously selected curated palette; changing mode never changes palette identity or Monet intent.
- Persisted appearance changes publish only after the preference write succeeds. A failed write keeps the last persisted theme visible and exposes a retryable Settings error state; it does not crash or discard unrelated Settings state.

### Presentation and ownership

- Settings presents System/Light/Dark as one accessible segmented control, four palette previews with an unmistakable selected state, and a Monet switch only when supported. The active source and fallback state are announced semantically, not by color alone.
- `:core:ui` owns the palette IDs, semantic color roles, deterministic resolution, and composition-local values. `:shared` owns preference persistence, Android host resolution, and composition. `:feature:settings` owns only the controls and receives immutable state/actions through its explicit public entry point.
- Because the Settings feature entry-point parameters change, update `docs/architecture.md` and `docs/adr/0001-feature-first-module-boundaries.md` in the same implementation change. Do not add a service locator or a second theme owner.

### Tokens and accessibility evidence

The authoritative color values, Miuix/Material3 role mappings, four-palette light/dark samples, WCAG contrast evidence, and Monet role mapping are in [the design token file](../../../docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.tokens.json). It contains 29 color roles plus `scrimAlpha` (30 fields) per palette/mode. All eight curated combinations meet 4.5:1 text/action/status pairs and 3:1 focus/essential-boundary pairs in the recorded surfaces. Dynamic samples are illustrative only; device/wallpaper and failure-path acceptance remains an Android runtime check.

### Implementation dependency

Complete this appearance contract before the adaptive shell consumes its tokens. The later redesign may change presentation of these values but does not redefine persistence, palette identity, Monet fallback, or selection semantics.
