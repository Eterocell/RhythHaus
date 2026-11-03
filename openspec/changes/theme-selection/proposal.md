## Why

RhythHaus currently has a single hardcoded light UI palette. Users need the app to adapt to light and dark contexts, and they need an explicit Settings choice for System, Light, and Dark that persists across restarts on Android, iOS, and macOS.

## What Changes

- Add a shared app appearance/theme mode model with System, Light, and Dark choices.
- Add DataStore Preferences-backed persistence for the selected theme mode across supported platforms.
- Add four complete Haus palettes (Moss, Bay, Clay, Iris), each with Light and Dark semantic tokens, and resolve them with the selected mode.
- Persist the selected palette independently from theme mode and expose an opt-in Android Monet preference on API 31+.
- Map supported Android dynamic color roles into the shared Haus palette while preserving a selected curated-palette fallback.
- Replace the Settings Appearance dropdown with a three-option mode segment, four palette previews, and the version-gated Monet switch.
- Migrate visible shared Compose surfaces from fixed light colors to the active Haus palette.
- Deliver a role-complete token table and WCAG contrast evidence before implementation.

## Capabilities

### New Capabilities
- `app-appearance`: persisted mode and curated-palette selection, optional Android dynamic color, and accessible shared theme adaptation.

### Modified Capabilities
- Settings gains appearance controls while retaining existing music management behavior.

## Impact

- `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/Theme.kt` and `HausColors.kt`: palette identities, complete semantic roles, deterministic resolution, and composition-local values.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.kt` and existing platform actuals: DataStore persistence for mode, palette, and Monet intent.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`: theme state collection, platform dynamic-color resolution, and Miuix/Material color-scheme mapping.
- `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/library/ui/LibraryAppShell.kt` and `LibraryRoutes.kt`: explicit Settings appearance state/action parameters.
- `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt` and its EN/ZH Compose resources: accessible mode, palette, and dynamic-color controls.
- `core/ui/src/commonTest`, `shared/src/jvmTest`, and `feature/settings/src/jvmTest`: palette, persistence, fallback, and production-route behavior tests.
- `docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.tokens.json`: authoritative palette values, role mappings, and contrast evidence.

## Non-goals

- No SQLDelight table or migration for theme settings.
- No native settings screens.
- No layout redesign outside Settings > Appearance; that section is redesigned to present the new theme choices.
- No playback, scanner, metadata, navigation, or library schema changes.
- No Windows/Linux product support.

## Verification

- `openspec validate theme-selection --strict`.
- Focused `:core:ui:jvmTest`, `:feature:settings:jvmTest`, and `:shared:jvmTest` coverage for palette resolution, persisted preferences, fallback behavior, and selected-state semantics.
- `./gradlew :shared:jvmTest :desktopApp:compileKotlin :androidApp:assembleDebug --configuration-cache`.
- `./gradlew :shared:iosSimulatorArm64Test --configuration-cache` when shared platform actuals are touched; complete the repository's full `./init.sh` verification before claiming implementation acceptance.
