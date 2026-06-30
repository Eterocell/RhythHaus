## Why

RhythHaus currently has a single hardcoded light UI palette. Users need the app to adapt to light and dark contexts, and they need an explicit Settings choice for System, Light, and Dark that persists across restarts on Android, iOS, and macOS.

## What Changes

- Add a shared app appearance/theme mode model with System, Light, and Dark choices.
- Add DataStore Preferences-backed persistence for the selected theme mode across supported platforms.
- Add light and dark Haus palettes and wire the shared Compose/Miuix theme to the selected/resolved mode.
- Add an Appearance section to Settings with System, Light, and Dark selection.
- Migrate visible shared Compose surfaces from fixed light colors to the active Haus palette.

## Capabilities

### New Capabilities
- `app-appearance`: persisted app theme selection and light/dark UI adaptation.

### Modified Capabilities
- Settings gains appearance controls while retaining existing music management behavior.

## Impact

- `gradle/libs.versions.toml`: DataStore dependency aliases.
- `shared/build.gradle.kts`: DataStore dependencies for common/platform source sets.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/Theme.kt`: theme mode, palettes, resolution, and composition local.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/ThemePreferenceStore.kt`: DataStore-backed preference API/factory seam.
- Platform actual files under `androidMain`, `iosMain`, and `jvmMain`: DataStore file path/factory support.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: theme state wiring and themed color migration.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/SettingsScreen.kt`: Appearance selector.
- Common/JVM tests for theme behavior and persistence.

## Non-goals

- No SQLDelight table or migration for theme settings.
- No native settings screens.
- No layout redesign.
- No playback, scanner, metadata, navigation, or library schema changes.
- No Windows/Linux product support.

## Verification

- `openspec validate theme-selection --strict`.
- Focused theme tests.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- iOS simulator tests when platform actuals are touched.
