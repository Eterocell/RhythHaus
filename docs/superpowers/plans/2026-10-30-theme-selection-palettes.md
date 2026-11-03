# Theme Palette Selection and Android Monet — Implementation Plan

Route: `openspec+superpowers`
Owner: implementation after user approval
Status: Draft; no production implementation has started
Change: `openspec/changes/theme-selection/`
Design input: `docs/superpowers/specs/2026-10-29-platform-adaptive-ui-redesign-design.md` and `docs/superpowers/designs/2026-10-29-platform-adaptive-ui-redesign.tokens.json`

## Goal

Extend the existing persisted System/Light/Dark setting with four complete curated palettes and opt-in Android API 31+ Monet. Keep mode, palette identity, and dynamic-color intent independent; route all common UI through one resolved Haus scheme. Maintain the existing DataStore owner and platform set. Do not alter playback, scanning, library behavior, SQLDelight schema, or feature dependency direction.

## Approval and execution constraints

- Review this plan together with the [adaptive UI implementation plan](2026-10-30-platform-adaptive-ui-redesign.md) in one upfront user-approval gate. Do not edit production code before approval.
- After approval, implement this change first in `.worktrees/theme-selection-palettes/`; preserve pre-existing worktree/index changes and inspect the worktree handoff before editing.
- Execute one dependency-ordered task at a time. Follow TDD; implementation tasks begin with a consumer-visible failing test at the public preference or Settings boundary. Do not test source text, forwarding, or a bare non-throw.
- Reuse `:core:ui` for theme roles, `:shared` for preference/platform orchestration, and `:feature:settings` for appearance controls. No service locator, compatibility alias, duplicate persistence owner, dependency, or database migration.

## Tasks

### 1. Establish complete palette and resolution contract

**Files:** `core/ui/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/Theme.kt`, `HausColors.kt`; `core/ui/src/commonTest/kotlin/com/eterocell/rhythhaus/theme/ThemeTest.kt`.

1. Add a stable `HausPaletteId` for Moss, Bay, Clay, and Iris. Keep `RhythHausThemeMode` and its current serialized values/default unchanged.
2. Expand `HausColorPalette` to the 29 color roles plus `scrimAlpha` from the token file. Encode all four palettes in Light and Dark; keep warning/success roles curated when dynamic colors are active.
3. Add a single immutable resolved-theme projection carrying palette ID, requested mode, resolved darkness, effective source, and complete roles. Resolve the System mode once and reuse `isDark` for curated and dynamic schemes.
4. Add common tests first for default/invalid palette IDs, all 30 fields per scheme, mode/palette independence, light/dark resolution, dynamic-role projection, and every specified contrast pair. Use boundary assertions against 4.5:1 text/action/status and 3:1 focus/essential boundaries; composite alpha roles against their declared surfaces.

**Acceptance:** all 8 curated schemes have exactly the 30 documented fields; invalid identifiers resolve to Moss without changing mode; fixed semantic roles and role mappings match the design token file; the consumer never infers darkness by comparing palette colors.

### 2. Extend persistence without replacing the existing owner

**Files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStore.kt`, existing Android/iOS/JVM actuals, `shared/src/jvmTest/kotlin/com/eterocell/rhythhaus/theme/ThemePreferenceStoreJvmTest.kt`.

1. Preserve `theme_mode` and extend the existing DataStore preference owner with stable `palette_id` and `dynamic_color_enabled` values.
2. Expose one immutable preference state/flow and explicit mode, palette, and Monet actions. Persist each action through the existing DataStore transaction boundary; selecting a curated palette also disables Monet.
3. Default missing keys to System, Moss, and Monet off. Parse unknown values safely. A supported-platform fallback must retain the Monet request; unsupported platform resolution must not silently erase it.
4. Write tests before implementation for legacy stores containing only `theme_mode`, invalid fields, restart restoration of all preferences, palette selection disabling Monet, and supported/unsupported effective-source behavior.
5. Verify failed writes leave the last persisted projection authoritative and do not mutate unrelated playback or navigation state; provide a retry path at the Settings boundary.

**Acceptance:** old users retain their stored mode; the new keys default independently; a re-created DataStore restores all three choices; failed writes do not publish an unsaved theme as selected.

### 3. Add Android dynamic-color provider and fallback

**Files:** Shared Android actual/provider under `shared/src/androidMain/kotlin/com/eterocell/rhythhaus/theme/`; common resolution contract under `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/theme/`; Shared Android host tests under the existing Android test source set.

1. Add an Android-only provider that reports availability only at API 31+ and reads the current light/dark platform dynamic scheme. Keep `Context`, Android `ColorScheme`, and API checks out of common and feature modules.
2. Map only roles in `androidDynamicSchemeToHaus`: background/surface, mapped surface containers, primary/on-primary and primary container pairs, text/inverse text, divider/boundary/focus, and error pairs. Warning/success continue to come from the selected curated palette.
3. Re-resolve on Android configuration/system appearance changes while the app is active. On provider failure, resolve the selected curated palette, mark effective source as curated, keep the request, and show the recoverable status in Settings.
4. Add deterministic common tests for mapping/fallback and Android host tests for API gating/provider behavior. Run API 29/30 and API 31+ runtime checks on actual supported Android environments; do not treat a sample JSON scheme as device evidence.

**Acceptance:** API 29/30 never expose Monet; API 31+ may opt in; system theme changes select matching dynamic light/dark roles without restart; the fallback palette and persisted request survive provider failure.

### 4. Thread appearance state through the existing feature boundary

**Files:** `shared/src/commonMain/kotlin/com/eterocell/rhythhaus/App.kt`, `library/ui/LibraryAppShell.kt`, `library/ui/LibraryRoutes.kt`; `feature/settings/src/commonMain/kotlin/com/eterocell/rhythhaus/settings/SettingsScreen.kt`; Settings EN/ZH resources.

1. Update `RhythHausTheme` to produce one Haus scheme and feed its roles to the existing Miuix and Material3 adapters. The selected `HausPaletteId` remains the fallback under Monet.
2. Collect persisted preference state in `App()` and pass immutable current values plus explicit callbacks through Shared to Settings. Keep platform dynamic-color access in Shared Android code.
3. Replace `AppearanceDropdown` with accessible mode segments, four palette previews, supported-Android Monet switch, current-source/fallback status, persistence error, and retry. Display selected state with semantic selection/state descriptions in addition to color.
4. Preserve settings scroll, current route, queue draft, playback, and other appearance values through a successful or failed selection.
5. Add production-route tests in `feature/settings/src/jvmTest/kotlin/com/eterocell/rhythhaus/settings/SettingsScreenSemanticsJvmTest.kt` and Shared integration tests for persistence/fallback state. Verify EN/ZH labels and the absent control on unsupported platforms.
6. Update `docs/architecture.md` and `docs/adr/0001-feature-first-module-boundaries.md` for the changed public Settings appearance entry point. Do not route dependencies through a service locator.

**Acceptance:** the production Settings route can change mode/palette/Monet; selection is accurate and accessible; unsupported platforms expose no dynamic-color action; failed persistence reports retry and restores the persisted appearance.

### 5. Verify and hand off

1. Run focused `:core:ui:jvmTest`, `:feature:settings:jvmTest`, and `:shared:jvmTest`; inspect generated test-result XML for executed counts/failures.
2. Run `:shared:testDebugUnitTest` for Android host tests, `:desktopApp:compileKotlin`, and `:androidApp:assembleDebug`.
3. Run `/usr/bin/xcrun xcodebuild -version` and `:shared:iosSimulatorArm64Test` for the common theme contract and non-Android fallback.
4. Run `./init.sh`, `openspec validate theme-selection --strict`, formatting/static/architecture gates, and a focused reviewer pass. Record exact output; never mark an unavailable platform passed.
5. Perform user-owned manual appearance, large-text, screen-reader, API 29/30 fallback, API 31+ wallpaper/system-theme, iOS, and macOS checks. Agent automation does not replace physical-device acceptance.
6. Update `progress.md` and `roadmap.md`, complete all new OpenSpec tasks, and commit the completed change with a conventional message only after review and the repository acceptance gate.
